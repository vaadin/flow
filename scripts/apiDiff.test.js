// Run with: node --test scripts/apiDiff.test.js
const test = require('node:test');
const assert = require('node:assert');
const { findComparedModules, selectReferenceVersion, collectChanges, findPullRequestChanges } = require('./apiDiff');

const PUBLISHED = [
  '24.10.14',
  '25.3.0-beta3',
  '25.3.0-rc2',
  '25.3.0',
  '25.3.3',
  '25.4.0-alpha2',
  '25.4.0-beta1',
  '25.4.0-rc1',
  '25.5.0-alpha1'
];

test('maintenance branch compares against its latest patch release', () => {
  assert.strictEqual(selectReferenceVersion('25.3-SNAPSHOT', PUBLISHED), '25.3.3');
});

test('branch under development compares against its latest beta or rc', () => {
  assert.strictEqual(selectReferenceVersion('25.4-SNAPSHOT', PUBLISHED), '25.4.0-rc1');
  assert.strictEqual(
    selectReferenceVersion(
      '25.4-SNAPSHOT',
      PUBLISHED.filter((v) => !v.includes('rc'))
    ),
    '25.4.0-beta1'
  );
});

test('branch with only alphas compares against the previous release', () => {
  assert.strictEqual(selectReferenceVersion('25.4-SNAPSHOT', PUBLISHED.slice(0, 6)), '25.3.3');
  assert.strictEqual(selectReferenceVersion('25.0-SNAPSHOT', PUBLISHED), '24.10.14');
});

test('a release is newer than its betas and rcs', () => {
  assert.strictEqual(selectReferenceVersion('25.4-SNAPSHOT', ['25.4.0-beta1', '25.4.0', '25.4.0-rc1']), '25.4.0');
});

test('modules without a jar or with flow.apicmp.skip are not compared', () => {
  const poms = {
    'pom.xml': '<modules><module>a</module><module>b</module><module>c</module><module>d</module></modules>',
    'a/pom.xml': '<packaging>jar</packaging>',
    'b/pom.xml': '<packaging>pom</packaging>',
    'c/pom.xml': '<properties><flow.apicmp.skip>true</flow.apicmp.skip></properties>',
    'd/pom.xml': '<artifactId>d</artifactId>'
  };
  assert.deepStrictEqual(
    findComparedModules((file) => poms[file] ?? null),
    ['a', 'd']
  );
});

const report = (...methods) => `<?xml version="1.0"?>
<japicmp>
  <classes>
    <class binaryCompatible="true" changeStatus="MODIFIED" fullyQualifiedName="com.example.Widget" sourceCompatible="true">
      <compatibilityChanges/>
      <fields/>
      <methods>${methods.join('')}</methods>
    </class>
  </classes>
</japicmp>`;
const method = (name, status, change, binaryCompatible, sourceCompatible = binaryCompatible) => `
        <method binaryCompatible="${binaryCompatible}" changeStatus="${status}" name="${name}" sourceCompatible="${sourceCompatible}">
          <compatibilityChanges>
            <compatibilityChange binaryCompatible="${binaryCompatible}" sourceCompatible="${sourceCompatible}" type="${change}"/>
          </compatibilityChanges>
          <parameters>
            <parameter changeStatus="${status}" type="java.lang.String&lt;T&gt;"/>
          </parameters>
        </method>`;
// The levels japicmp gives the change types used here
const LEVELS = new Map([
  ['METHOD_ADDED_TO_PUBLIC_CLASS', '0.0.1'],
  ['ANNOTATION_DEPRECATED_ADDED', '0.1.0'],
  ['METHOD_ADDED_TO_INTERFACE', '0.1.0'],
  ['METHOD_REMOVED', '1.0.0']
]);
const added = (name) => method(name, 'NEW', 'METHOD_ADDED_TO_PUBLIC_CLASS', true);
const removed = (name) => method(name, 'REMOVED', 'METHOD_REMOVED', false);

test('removing a method added after the release is not a change', () => {
  const base = collectChanges(report(added('merged'), added('unreleased')), LEVELS);
  const head = collectChanges(report(added('merged')), LEVELS);
  assert.deepStrictEqual([...findPullRequestChanges(base, head)], []);
  assert.deepStrictEqual(
    [...findPullRequestChanges(head, base).keys()],
    ['com.example.Widget#unreleased(java.lang.String<T>): METHOD_ADDED_TO_PUBLIC_CLASS']
  );
});

test('only the changes the pull request adds on top of the merge base count', () => {
  const base = collectChanges(report(added('merged')), LEVELS);
  const head = collectChanges(report(added('merged'), added('added'), removed('released')), LEVELS);
  assert.deepStrictEqual(
    [...findPullRequestChanges(base, head)],
    [
      ['com.example.Widget#added(java.lang.String<T>): METHOD_ADDED_TO_PUBLIC_CLASS', '0.0.1'],
      ['com.example.Widget#released(java.lang.String<T>): METHOD_REMOVED', '1.0.0']
    ]
  );
});

test('changes have the level of their type, whatever their compatibility', () => {
  const changes = collectChanges(
    report(
      method('added', 'NEW', 'METHOD_ADDED_TO_PUBLIC_CLASS', true),
      method('deprecated', 'MODIFIED', 'ANNOTATION_DEPRECATED_ADDED', true),
      method('abstract', 'NEW', 'METHOD_ADDED_TO_INTERFACE', true, false)
    ),
    LEVELS
  );
  assert.deepStrictEqual([...changes.values()], ['0.0.1', '0.1.0', '0.1.0']);
});

test('a change without a compatibility change is a patch, and a new class is one change', () => {
  const changes = collectChanges(
    `<?xml version="1.0"?>
<japicmp>
  <classes>
    <class changeStatus="NEW" fullyQualifiedName="com.example.Added">
      <compatibilityChanges/>
      <methods>${added('run')}</methods>
    </class>
    <class changeStatus="MODIFIED" fullyQualifiedName="com.example.Widget">
      <compatibilityChanges/>
      <fields>
        <field changeStatus="NEW" name="count"><compatibilityChanges/></field>
        <field changeStatus="MODIFIED" name="size"><compatibilityChanges/></field>
      </fields>
    </class>
  </classes>
</japicmp>`,
    LEVELS
  );
  assert.deepStrictEqual(
    [...changes],
    [
      ['com.example.Added: NEW', '0.0.1'],
      ['com.example.Widget#count: NEW', '0.0.1'],
      ['com.example.Widget#size: MODIFIED', '0.0.1']
    ]
  );
});
