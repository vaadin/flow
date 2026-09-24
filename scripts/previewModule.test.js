// Run with: node --test scripts/previewModule.test.js
const test = require('node:test');
const assert = require('node:assert');
const { selectModule, viewPaths, comment } = require('./previewModule');

const DEFAULT = 'flow-tests/test-default';
const MAIN = 'src/main/java/com/vaadin/flow/uitest/ui';
const TEST = 'src/test/java/com/vaadin/flow/uitest/ui';

test('framework-only change deploys test-default', () => {
  assert.strictEqual(selectModule(['flow-server/src/main/java/com/vaadin/flow/Foo.java']), DEFAULT);
});

test('test-default wins over other changed test modules', () => {
  assert.strictEqual(
    selectModule([
      `flow-tests/test-root-context/${MAIN}/AView.java`,
      `flow-tests/test-root-context/${MAIN}/BView.java`,
      `${DEFAULT}/src/main/java/com/vaadin/flow/test/CView.java`
    ]),
    DEFAULT
  );
});

test('only another test module changed deploys that module', () => {
  assert.strictEqual(
    selectModule(['flow-server/src/main/java/com/vaadin/flow/Foo.java', `flow-tests/test-misc/${MAIN}/AView.java`]),
    'flow-tests/test-misc'
  );
});

test('several test modules: most changed files, then name', () => {
  assert.strictEqual(
    selectModule([
      `flow-tests/test-misc/${MAIN}/AView.java`,
      `flow-tests/test-themes/${MAIN}/AView.java`,
      `flow-tests/test-themes/${TEST}/AIT.java`
    ]),
    'flow-tests/test-themes'
  );
  assert.strictEqual(
    selectModule([`flow-tests/test-themes/${MAIN}/AView.java`, `flow-tests/test-misc/${MAIN}/AView.java`]),
    'flow-tests/test-misc'
  );
});

test('non-deployable test modules fall back to test-default', () => {
  assert.strictEqual(
    selectModule([
      'flow-tests/test-live-reload/src/main/java/Foo.java',
      'flow-tests/test-common/src/main/java/Bar.java',
      // Prefix of a deployable module name, but a different module
      'flow-tests/test-pwa-disabled-offline-x/pom.xml'
    ]),
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
      module,
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
      module,
      [`${module}/${TEST}/RoutedIT.java`, `${module}/${TEST}/PathIT.java`, `${module}/${TEST}/ComputedPathIT.java`],
      (file) => sources[file] ?? null
    ),
    ['/foo/param/a%2bb', '/foo/view/com.vaadin.flow.uitest.ui.RoutedView']
  );
});

test('comment names the module and links to its root and views', () => {
  const body = comment('flow-tests/test-ccdm', ['/foo/view/a.B'], 'https://p.fly.dev');
  assert.match(body, /Deployed `flow-tests\/test-ccdm`: https:\/\/p\.fly\.dev\/foo\//);
  assert.match(body, /- https:\/\/p\.fly\.dev\/foo\/view\/a\.B/);
});
