import assert from 'node:assert/strict';
import { describe, test } from 'node:test';
import {
  buildDiff,
  classify,
  evaluatePolicy,
  isTrustedAuthor,
  isTrustedHead,
  linkedIssueRefs,
  renderComment,
} from '../classify.js';

function file(filename, patch = '@@ -1 +1 @@\n-a\n+b', extra = {}) {
  return { filename, status: 'modified', additions: 1, deletions: 1, changes: 2, patch, ...extra };
}

function added(...lines) {
  return `@@ -1 +1,${lines.length} @@\n${lines.map((line) => `+${line}`).join('\n')}`;
}

const removed = (filename) => file(filename, '@@ -1 +0,0 @@\n-a', { status: 'removed' });

/** The findings as `level: reason` */
function policy(files, options = {}) {
  return evaluatePolicy({
    author: 'dev',
    title: 'fix: change something',
    body: '',
    authorTrusted: true,
    headTrusted: true,
    files,
    ...options,
  }).map(({ level, reason }) => `${level}: ${reason}`);
}

const SERVER = 'flow-server/src/main/java/com/vaadin/flow';
const AUTOMATION = 'high: CI, release and repository automation, or config that runs code';
const CHECKS = 'medium: CI checks that change no secrets, permissions, triggers or actions';
const CORE = 'medium: core framework code that every application runs';
const INTEGRATION = 'medium: Spring, CDI and Quarkus integration that every application on that stack runs';
const TOOLING = 'medium: build plugins and dev tooling that every application build runs';
const SINK = 'high: adds an HTML or script injection sink';
const NETWORK = 'high: adds network requests';
const STORAGE = 'high: adds browser storage or cookies';

describe('evaluatePolicy', () => {
  test('leaves tests and docs alone', () => {
    assert.deepEqual(policy([file(`flow-server/src/test/java/com/vaadin/flow/dom/ElementTest.java`)]), []);
    assert.deepEqual(policy([file('flow-tests/test-root-context/src/main/java/A.java')]), []);
    assert.deepEqual(policy([file('README.md'), file('.github/workflows/README.md')]), []);
  });

  test('sets high for security code, but not for Javadoc changes to it', () => {
    const since = '@@ -1,2 +1,3 @@\n      *\n+     * @since 25.4\n      */';
    for (const [path, reason] of [
      [`${SERVER}/server/auth/NavigationContext.java`, 'access control, authentication and CSRF protection'],
      [`${SERVER}/server/communication/rpc/MapSyncRpcHandler.java`, 'handling of client requests and RPC input'],
      [`${SERVER}/server/streams/FileUploadHandler.java`, 'file uploads to the server'],
      [`${SERVER}/internal/UsageStatistics.java`, 'usage statistics'],
      [`${SERVER}/server/startup/LicenseChecker.java`, 'licensing'],
    ]) {
      assert.deepEqual(policy([file(path)]), [`high: ${reason}`, CORE], path);
      assert.deepEqual(policy([file(path, since)]), [], path);
    }
    assert.deepEqual(policy([file('vaadin-spring/src/main/java/com/vaadin/flow/spring/security/A.java')]), [
      'high: access control, authentication and CSRF protection',
      INTEGRATION,
    ]);
  });

  test('sets medium for core code, integrations and dev tooling', () => {
    assert.deepEqual(policy([file(`${SERVER}/dom/Element.java`)]), [CORE]);
    assert.deepEqual(policy([file('flow-client/src/main/frontend/Flow.ts')]), [CORE]);
    assert.deepEqual(policy([file('vaadin-cdi/src/main/java/com/vaadin/cdi/A.java')]), [INTEGRATION]);
    assert.deepEqual(policy([file('vaadin-quarkus/runtime/src/main/java/A.java')]), [INTEGRATION]);
    assert.deepEqual(policy([file('flow-devloop-daemon/src/main/java/Launch.java')]), [TOOLING]);
    assert.deepEqual(policy([file('guidelines/testing.md')]), [
      'medium: instructions that agents follow, including the agent review',
    ]);
  });

  test('sets medium for dependencies, but not for integration test modules', () => {
    assert.deepEqual(policy([file('flow-server/pom.xml')]), ['medium: dependencies']);
    assert.deepEqual(policy([file('flow-tests/pom.xml'), file('flow-tests/test-csp/pom.xml')]), []);
  });

  describe('CI', () => {
    const edit = (line) => `@@ -1 +1 @@\n-      run: ./old.sh\n+${line}`;

    test('sets medium for checks that change no secrets, permissions, triggers or actions', () => {
      assert.deepEqual(policy([file('.github/workflows/validation.yml', edit('      run: ./mvnw -B verify'))]), [CHECKS]);
      assert.deepEqual(policy([file('scripts/runMavenWithRetry.sh', added('for attempt in 1 2 3; do'))]), [CHECKS]);
    });

    test('sets high when a line touches secrets, permissions, triggers, actions, PR text or the API diff', () => {
      for (const line of [
        '          GH_TOKEN: ${{ secrets.VAADIN_BOT_TOKEN }}',
        '      pull-requests: write',
        'on:',
        '      - uses: some/action@v1',
        '      run: echo "${{ github.event.pull_request.title }}"',
        '          version_change="0.0.1"',
        '      run: node scripts/apiDiff.js',
      ]) {
        assert.deepEqual(policy([file('.github/workflows/validation.yml', edit(line))]), [AUTOMATION], line);
      }
    });

    test('keeps the merge gates, agent workflows and other repository config high', () => {
      for (const path of [
        '.github/workflows/pr-risk.yml',
        '.github/workflows/doc-bot.md',
        'scripts/autoMerge.js',
        'scripts/apiDiff.js',
        '.github/CODEOWNERS',
        '.mvn/wrapper/maven-wrapper.properties',
      ]) {
        assert.deepEqual(policy([file(path, edit('      run: ./new.sh'))]), [AUTOMATION], path);
      }
      // A file without a diff cannot be checked
      assert.deepEqual(policy([file('scripts/runMavenWithRetry.sh', null)]), [AUTOMATION]);
    });

    test('sets medium for a removed workflow', () => {
      assert.deepEqual(policy([removed('.github/workflows/claude.yml')]), ['medium: removes CI workflows']);
      assert.deepEqual(policy([removed('.github/CODEOWNERS')]), [AUTOMATION]);
    });
  });

  describe('Dependabot patch updates', () => {
    const TITLE = 'chore(deps-dev): bump org.jacoco:jacoco-maven-plugin from 0.8.15 to 0.8.16';
    const BUMP = 'low: a patch update of a build or test tool by Dependabot';
    const jacoco = (from = '0.8.15', to = '0.8.16') =>
      file(
        'pom.xml',
        `@@ -128,3 +128,3 @@\n-    <jacoco.plugin.version>${from}</jacoco.plugin.version>\n+    <jacoco.plugin.version>${to}</jacoco.plugin.version>`,
      );
    const bump = (files, options = {}) => policy(files, { author: 'dependabot[bot]', title: TITLE, ...options });

    test('skips the dependency rule for a patch update that only changes the version', () => {
      assert.deepEqual(bump([jacoco()]), [BUMP]);
    });

    test('keeps it for shipped dependencies, minor updates, other authors and other changes', () => {
      assert.deepEqual(bump([jacoco()], { title: TITLE.replace('deps-dev', 'deps') }), ['medium: dependencies']);
      assert.deepEqual(bump([jacoco('0.8.15', '0.9.0')], { title: TITLE.replace('0.8.16', '0.9.0') }), [
        'medium: dependencies',
      ]);
      assert.deepEqual(bump([jacoco()], { author: 'someone' }), ['medium: dependencies']);
      const extra = file('pom.xml', `${jacoco().patch}\n+    <argLine>-javaagent:evil.jar</argLine>`);
      assert.deepEqual(bump([extra]), ['medium: dependencies']);
    });
  });

  describe('added code', () => {
    test('finds injection sinks, network requests and storage in shipped code only', () => {
      const ts = 'flow-client/src/main/frontend/Flow.ts';
      assert.deepEqual(policy([file(ts, added('el.innerHTML = html;'))]), [CORE, SINK]);
      assert.deepEqual(policy([file(ts, added("el.innerHTML = '';"))]), [CORE]);
      assert.deepEqual(policy([file(ts, added('await fetch(url);'))]), [CORE, NETWORK]);
      assert.deepEqual(policy([file(ts, added('localStorage.setItem(k, v);'))]), [CORE, STORAGE]);
      assert.deepEqual(policy([file('flow-client/src/test/frontend/FlowTests.ts', added('await fetch(url);'))]), []);
    });

    test('treats Java comments, property reads and plain Java calls as harmless', () => {
      const element = `${SERVER}/dom/Element.java`;
      assert.deepEqual(policy([file(element, added('     * HTML set through "innerHTML"'))]), []);
      assert.deepEqual(policy([file(element, added('return getProperty("innerHTML");'))]), [CORE]);
      assert.deepEqual(policy([file(element, added('return dataProvider.fetch(query);'))]), [CORE]);
      assert.deepEqual(policy([file(element, added('executeJs("return fetch($0)", url);'))]), [CORE, NETWORK]);
    });

    test('skips storage, but not network requests, in the dev server', () => {
      const dev = 'vaadin-dev-server/src/main/frontend/hotswap-scroll.ts';
      assert.deepEqual(policy([file(dev, added('sessionStorage.setItem(k, v);'))]), [TOOLING]);
      assert.deepEqual(policy([file(dev, added('await fetch(url);'))]), [TOOLING, NETWORK]);
    });

    test('sets medium when files move into shipped code', () => {
      const moved = file(`${SERVER}/A.java`, null, { status: 'renamed', previous_filename: 'docs/A.java' });
      assert.deepEqual(policy([moved]), [
        CORE,
        'medium: moves files into shipped code, so the rules did not check all of their content',
      ]);
    });
  });

  test('reads the title, description, author, head branch and linked issues', () => {
    assert.deepEqual(policy([], { title: 'feat!: x' }), ['high: breaking change (`!` in the title)']);
    assert.deepEqual(policy([], { body: 'BREAKING CHANGE: x' }), [
      'high: breaking change (`BREAKING CHANGE:` in the description)',
    ]);
    assert.deepEqual(policy([], { authorTrusted: false, headTrusted: false }), [
      'medium: the author is not a member of the organization',
      'medium: the head branch is in a fork that the author does not own',
    ]);
    const linkedIssues = [
      { ref: 'vaadin/flow#1', labels: ['Impact: High'] },
      { ref: 'vaadin/flow#2', labels: ['Impact: Low'] },
    ];
    assert.deepEqual(evaluatePolicy({ title: 'fix: x', authorTrusted: true, headTrusted: true, files: [], linkedIssues }), [
      {
        level: 'medium',
        reason: 'a linked issue is labelled `Impact: High`: the problem reaches many applications',
        files: ['vaadin/flow#1'],
      },
    ]);
  });
});

describe('linkedIssueRefs', () => {
  const repo = { owner: 'vaadin', repo: 'flow' };

  test('finds issues of the same owner that the description fixes or is part of, once each', () => {
    const body = [
      'Fixes #951',
      'Part of vaadin/platform#12',
      'Closes https://github.com/vaadin/flow/issues/7, fixes #951',
      'Fixes other/repo#3',
      'See #8',
    ].join('\n');
    assert.deepEqual(linkedIssueRefs(body, repo), [
      { owner: 'vaadin', repo: 'flow', number: 951 },
      { owner: 'vaadin', repo: 'platform', number: 12 },
      { owner: 'vaadin', repo: 'flow', number: 7 },
    ]);
    assert.deepEqual(linkedIssueRefs(null, repo), []);
  });
});

describe('trust', () => {
  test('trusts members, collaborators and bots', () => {
    assert.equal(isTrustedAuthor({ authorAssociation: 'MEMBER', isMember: false }), true);
    assert.equal(isTrustedAuthor({ authorAssociation: 'CONTRIBUTOR', isMember: true }), true);
    assert.equal(isTrustedAuthor({ authorAssociation: 'NONE', isMember: false, isBot: true }), true);
    assert.equal(isTrustedAuthor({ authorAssociation: 'CONTRIBUTOR', isMember: false }), false);
  });

  test('trusts a head in the base repository or in a fork of the author', () => {
    const base = { repo: { full_name: 'vaadin/flow' } };
    const head = (name) => ({ repo: { full_name: name, owner: { login: name.split('/')[0] } } });
    assert.equal(isTrustedHead({ base, head: head('vaadin/flow'), user: { login: 'dev' } }), true);
    assert.equal(isTrustedHead({ base, head: head('dev/flow'), user: { login: 'dev' } }), true);
    assert.equal(isTrustedHead({ base, head: head('other/flow'), user: { login: 'dev' } }), false);
    assert.equal(isTrustedHead({ base, head: { repo: null }, user: { login: 'dev' } }), false);
  });
});

describe('buildDiff', () => {
  test('puts lockfiles last and lists what it leaves out', () => {
    const files = [
      file('flow-client/package-lock.json', '@@\n+lock'),
      file('A.java', '@@\n+a'),
      file('image.png', null, { status: 'added' }),
      file('gone.java', null, { status: 'removed' }),
      file('empty.txt', null, { status: 'added', sha: 'e69de29bb2d1d6434b8b29ae775ad8c2e48c5391' }),
    ];
    const { text, omitted } = buildDiff(files);
    assert.ok(text.indexOf('A.java') < text.indexOf('package-lock.json'));
    assert.deepEqual(omitted, ['image.png']);
    assert.deepEqual(buildDiff([file('A.java', '@@\n+a'.repeat(50))], 100).omitted, ['A.java']);
  });
});

const rating = (level) => ({ level, reason: `rated ${level}` });
function assessment(level, levels = {}) {
  return {
    summary: 'Changes something.',
    impact: rating(levels.impact ?? 'low'),
    blast_radius: rating(levels.blast_radius ?? 'low'),
    uncertainty: rating(levels.uncertainty ?? 'low'),
    reversibility: rating(levels.reversibility ?? 'low'),
    level,
    rationale: 'Because.',
    model: 'claude-test',
  };
}

describe('classify', () => {
  const none = { policyFindings: [], omitted: [] };

  test('takes the higher of the suggestion and the rules', () => {
    assert.deepEqual(classify({ ...none, assessment: assessment('low') }), { level: 'low', findings: [] });
    const policyFindings = [{ level: 'high', reason: 'CI', files: [] }];
    assert.equal(classify({ ...none, policyFindings, assessment: assessment('low') }).level, 'high');
  });

  test('keeps low and high honest', () => {
    assert.equal(classify({ ...none, assessment: assessment('low', { impact: 'medium' }) }).level, 'medium');
    assert.equal(classify({ ...none, assessment: assessment('high', { impact: 'medium' }) }).level, 'medium');
    assert.equal(classify({ ...none, assessment: assessment('high', { impact: 'high' }) }).level, 'high');
  });

  test('sets medium without a full assessment', () => {
    assert.equal(classify({ ...none, assessment: null }).level, 'medium');
    assert.equal(classify({ ...none, assessment: null, skipped: true }).level, 'medium');
    assert.deepEqual(classify({ ...none, assessment: null, skipped: true }).findings, []);
    assert.equal(classify({ ...none, omitted: ['big.bin'], assessment: assessment('low') }).level, 'medium');
  });
});

describe('renderComment', () => {
  const base = { marker: '<!-- m -->', headSha: '0123456789abcdef', runUrl: 'https://example.com/run' };

  test('explains the level, the ratings and the rules', () => {
    const findings = [{ level: 'high', reason: 'CI', files: ['.github/x.yml'] }];
    const body = renderComment({ ...base, result: { level: 'high', findings }, assessment: assessment('medium') });
    assert.ok(body.startsWith('<!-- m -->\n### 🧪 Risk evaluation (trial): 🔴 high risk\n'));
    assert.match(body, /suggested \*\*medium\*\*\. The rules below raised it to \*\*high\*\*\. Changes something\./u);
    assert.match(body, /\| Blast radius \| 🟢 low \| rated low \|/u);
    assert.match(body, /- 🔴 \*\*high\*\*: CI\n {2}- `\.github\/x\.yml`/u);
  });

  test('says when a high suggestion counts as medium', () => {
    const body = renderComment({ ...base, result: { level: 'medium', findings: [] }, assessment: assessment('high') });
    assert.match(body, /suggested \*\*high\*\*\. It rated no dimension high, so it counts as \*\*medium\*\*\./u);
  });

  test('keeps model text from pinging, linking or breaking the table', () => {
    const steered = assessment('medium');
    steered.summary = 'Ping @octocat, click [Approve](https://evil.example/x) or ![x](https://evil.example/p.png)';
    steered.rationale = '<a href="https://evil.example">approve</a> at www.evil.example';
    steered.impact.reason = 'a | b';
    const body = renderComment({ ...base, result: { level: 'medium', findings: [] }, assessment: steered });
    const modelText = body.split('<sub>')[0];
    assert.doesNotMatch(modelText, /(?<!\\)\]\(|(?<!\\)<a |https?:\/\/evil|www\.evil|@octocat/u);
    assert.match(body, /\| a \\\| b \|/u);
  });

  test('says why the agent did not rate the pull request', () => {
    const result = { level: 'medium', findings: [] };
    assert.match(renderComment({ ...base, result, assessment: null }), /assessment failed, see the \[workflow run\]/u);
    assert.match(
      renderComment({ ...base, result, assessment: null, skipped: true }),
      /does not assess pull requests from authors outside the organization/u,
    );
  });
});
