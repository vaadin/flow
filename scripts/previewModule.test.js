// Run with: node --test scripts/previewModule.test.js
const test = require('node:test');
const assert = require('node:assert');
const { selectDeployment, viewPaths, comment } = require('./previewModule');

const DEFAULT = 'flow-tests/test-default';
const MAIN = 'src/main/java/com/vaadin/flow/uitest/ui';
const TEST = 'src/test/java/com/vaadin/flow/uitest/ui';

// A flow-tests reactor with one module of each kind the poms tell apart
const POMS = {
  'flow-tests/pom.xml':
    '<modules><module>test-common</module></modules>\n<profiles>\n' +
    '<profile><id>it-test-modules</id><modules>\n' +
    ['test-default', 'test-misc', 'test-themes', 'test-ccdm', 'test-encoded']
      .map((name) => `<module>${name}</module>`)
      .join('\n') +
    '\n</modules></profile>\n' +
    '<profile><id>nightly</id><modules><module>test-nightly</module></modules></profile>\n' +
    '</profiles>',
  [`${DEFAULT}/pom.xml`]: '<packaging>jar</packaging>\n<artifactId>spring-boot-maven-plugin</artifactId>',
  'flow-tests/test-misc/pom.xml': '<packaging>war</packaging>\n<artifactId>jetty-ee10-maven-plugin</artifactId>',
  'flow-tests/test-themes/pom.xml': '<packaging>war</packaging>\n<artifactId>jetty-ee10-maven-plugin</artifactId>',
  'flow-tests/test-ccdm/pom.xml':
    '<packaging>war</packaging>\n<artifactId>jetty-ee10-maven-plugin</artifactId>\n' +
    '<contextPath>/foo</contextPath>',
  'flow-tests/test-encoded/pom.xml':
    '<packaging>war</packaging>\n<artifactId>jetty-maven-plugin</artifactId>\n' +
    '<contextPath>${jettyContextPath}</contextPath>',
  'flow-tests/test-common/pom.xml': '<packaging>jar</packaging>',
  // Wars outside the built reactor: only in a profile it leaves out, or not
  // a module of flow-tests at all
  'flow-tests/test-nightly/pom.xml': '<packaging>war</packaging>\n<artifactId>jetty-ee10-maven-plugin</artifactId>',
  'flow-tests/test-orphan/pom.xml': '<packaging>war</packaging>\n<artifactId>jetty-ee10-maven-plugin</artifactId>'
};
const select = (files) => selectDeployment(files, (file) => POMS[file] ?? null);

test('framework-only change deploys test-default', () => {
  assert.deepStrictEqual(select(['flow-server/src/main/java/com/vaadin/flow/Foo.java']), {
    module: DEFAULT,
    contextPath: ''
  });
});

test('test-default wins over other changed test modules', () => {
  assert.strictEqual(
    select([
      `flow-tests/test-misc/${MAIN}/AView.java`,
      `flow-tests/test-misc/${MAIN}/BView.java`,
      `${DEFAULT}/src/main/java/com/vaadin/flow/test/CView.java`
    ]).module,
    DEFAULT
  );
});

test('only another test module changed deploys it at its context path', () => {
  assert.deepStrictEqual(
    select(['flow-server/src/main/java/com/vaadin/flow/Foo.java', `flow-tests/test-ccdm/${MAIN}/AView.java`]),
    { module: 'flow-tests/test-ccdm', contextPath: '/foo' }
  );
});

test('several test modules: most changed files, then name', () => {
  assert.strictEqual(
    select([
      `flow-tests/test-misc/${MAIN}/AView.java`,
      `flow-tests/test-themes/${MAIN}/AView.java`,
      `flow-tests/test-themes/${TEST}/AIT.java`
    ]).module,
    'flow-tests/test-themes'
  );
  assert.strictEqual(
    select([`flow-tests/test-themes/${MAIN}/AView.java`, `flow-tests/test-misc/${MAIN}/AView.java`]).module,
    'flow-tests/test-misc'
  );
});

test('modules the preview cannot start fall back to test-default', () => {
  assert.strictEqual(
    select([
      `flow-tests/test-common/${MAIN}/Foo.java`,
      `flow-tests/test-encoded/${MAIN}/AView.java`,
      `flow-tests/test-nightly/${MAIN}/AView.java`,
      `flow-tests/test-orphan/${MAIN}/AView.java`,
      'flow-tests/pom.xml'
    ]).module,
    DEFAULT
  );
});

test('links to views by @Route outside ViewTestServlet modules', () => {
  const module = 'flow-tests/test-default';
  const pkg = 'src/main/java/com/vaadin/flow/test';
  const sources = {
    [`${module}/pom.xml`]: '<artifactId>vaadin-spring</artifactId>',
    [`${module}/${pkg}/RoutedView.java`]: '@Route("stream-resource")\npublic class RoutedView extends Div {}',
    [`${module}/${pkg}/ConstantView.java`]:
      '@Route(ConstantView.ROUTE)\npublic class ConstantView extends Div {\n' +
      '    public static final String ROUTE = "login";\n}',
    [`${module}/${pkg}/UnroutedView.java`]: 'public class UnroutedView extends Div {}',
    [`${module}/${pkg}/Helper.java`]: 'public final class Helper {}',
    [`${module}/src/test/java/com/vaadin/flow/test/other/TargetIT.java`]:
      'import com.vaadin.flow.test.RoutedView;\n' +
      '@TestFor(RoutedView.class)\npublic class TargetIT extends AbstractDefaultIT {}'
  };
  assert.deepStrictEqual(
    viewPaths(
      { module, contextPath: '' },
      [
        `${module}/src/test/java/com/vaadin/flow/test/other/TargetIT.java`,
        `${module}/${pkg}/ConstantView.java`,
        `${module}/${pkg}/UnroutedView.java`,
        `${module}/${pkg}/Helper.java`,
        'flow-tests/test-misc/src/main/java/OtherView.java'
      ],
      (file) => sources[file] ?? null
    ),
    ['/login', '/stream-resource']
  );
});

test('links to views by class name in ViewTestServlet modules', () => {
  const module = 'flow-tests/test-ccdm';
  const sources = {
    [`${module}/pom.xml`]: '<artifactId>flow-test-common</artifactId>',
    [`${module}/${MAIN}/RoutedView.java`]:
      '@Route(value = "com.vaadin.flow.uitest.ui.RoutedView", layout = ViewTestLayout.class)\n' +
      'public class RoutedView extends Div {}',
    [`${module}/${TEST}/RoutedIT.java`]: 'public class RoutedIT extends ChromeBrowserTest {}',
    [`${module}/${TEST}/PathIT.java`]:
      'public class PathIT extends ChromeBrowserTest {\n' +
      '    protected String getTestPath() {\n        return "/foo/param/a%2bb";\n    }\n}',
    [`${module}/${TEST}/ComputedPathIT.java`]:
      'public class ComputedPathIT extends ChromeBrowserTest {\n' +
      '    protected String getTestPath() {\n        return JETTY_CONTEXT + "/x";\n    }\n}'
  };
  assert.deepStrictEqual(
    viewPaths(
      { module, contextPath: '/foo' },
      [`${module}/${TEST}/RoutedIT.java`, `${module}/${TEST}/PathIT.java`, `${module}/${TEST}/ComputedPathIT.java`],
      (file) => sources[file] ?? null
    ),
    ['/foo/param/a%2bb', '/foo/view/com.vaadin.flow.uitest.ui.RoutedView']
  );
});

test('comment names the module and links to its root and views', () => {
  const body = comment({ module: 'flow-tests/test-ccdm', contextPath: '/foo' }, ['/foo/view/a.B'], 'https://p.fly.dev');
  assert.match(body, /Deployed `flow-tests\/test-ccdm`: https:\/\/p\.fly\.dev\/foo\//);
  assert.match(body, /- https:\/\/p\.fly\.dev\/foo\/view\/a\.B/);
});
