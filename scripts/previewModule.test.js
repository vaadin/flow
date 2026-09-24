// Run with: node --test scripts/previewModule.test.js
const test = require('node:test');
const assert = require('node:assert');
const { selectModule, viewPaths, comment } = require('./previewModule');

const DEFAULT = 'flow-tests/test-default';
const MAIN = 'src/main/java/com/vaadin/flow/uitest/ui';
const TEST = 'src/test/java/com/vaadin/flow/uitest/ui';

test('framework-only change deploys test-default', () => {
  assert.strictEqual(
    selectModule(['flow-server/src/main/java/com/vaadin/flow/Foo.java']),
    DEFAULT,
  );
});

test('test-default wins over other changed test modules', () => {
  assert.strictEqual(
    selectModule([
      `flow-tests/test-root-context/${MAIN}/AView.java`,
      `flow-tests/test-root-context/${MAIN}/BView.java`,
      `${DEFAULT}/src/main/java/com/vaadin/flow/test/CView.java`,
    ]),
    DEFAULT,
  );
});

test('only another test module changed deploys that module', () => {
  assert.strictEqual(
    selectModule([
      'flow-server/src/main/java/com/vaadin/flow/Foo.java',
      `flow-tests/test-misc/${MAIN}/AView.java`,
    ]),
    'flow-tests/test-misc',
  );
});

test('several test modules: most changed files, then name', () => {
  assert.strictEqual(
    selectModule([
      `flow-tests/test-misc/${MAIN}/AView.java`,
      `flow-tests/test-themes/${MAIN}/AView.java`,
      `flow-tests/test-themes/${TEST}/AIT.java`,
    ]),
    'flow-tests/test-themes',
  );
  assert.strictEqual(
    selectModule([
      `flow-tests/test-themes/${MAIN}/AView.java`,
      `flow-tests/test-misc/${MAIN}/AView.java`,
    ]),
    'flow-tests/test-misc',
  );
});

test('non-deployable test modules fall back to test-default', () => {
  assert.strictEqual(
    selectModule([
      'flow-tests/test-live-reload/src/main/java/Foo.java',
      'flow-tests/test-common/src/main/java/Bar.java',
      // Prefix of a deployable module name, but a different module
      'flow-tests/test-pwa-disabled-offline-x/pom.xml',
    ]),
    DEFAULT,
  );
});

test('links to changed views and to the views of changed ITs', () => {
  const module = 'flow-tests/test-root-context';
  const sources = {
    [`${module}/${MAIN}/RoutedView.java`]:
      '@Route(value = "com.vaadin.flow.uitest.ui.RoutedView", layout = ViewTestLayout.class)\n' +
      'public class RoutedView extends Div {}',
    [`${module}/${MAIN}/PlainView.java`]:
      'public class PlainView extends AbstractDivView {}',
    [`${module}/${MAIN}/ConstantView.java`]:
      '@Route(ConstantView.ROUTE)\npublic class ConstantView extends Div {\n' +
      '    public static final String ROUTE = "login";\n}',
    [`${module}/${TEST}/PlainIT.java`]: 'public class PlainIT extends ChromeBrowserTest {}',
    [`${module}/${TEST}/other/TargetIT.java`]:
      'import com.vaadin.flow.uitest.ui.RoutedView;\n' +
      '@TestFor(RoutedView.class)\npublic class TargetIT extends AbstractIT {}',
    [`${module}/${MAIN}/Helper.java`]: 'public final class Helper {}',
  };
  const read = (file) => sources[file] ?? null;
  assert.deepStrictEqual(
    viewPaths(
      module,
      [
        `${module}/${TEST}/PlainIT.java`,
        `${module}/${TEST}/other/TargetIT.java`,
        `${module}/${MAIN}/ConstantView.java`,
        `${module}/${MAIN}/Helper.java`,
        `${module}/${TEST}/NoViewIT.java`,
        'flow-tests/test-misc/src/main/java/OtherView.java',
      ],
      read,
    ),
    ['/com.vaadin.flow.uitest.ui.RoutedView', '/login', '/view/com.vaadin.flow.uitest.ui.PlainView'],
  );
});

test('comment names the module and links under its context path', () => {
  const body = comment('flow-tests/test-ccdm', ['/view/a.B'], 'https://p.fly.dev');
  assert.match(body, /Deployed `flow-tests\/test-ccdm`: https:\/\/p\.fly\.dev\/foo\//);
  assert.match(body, /- https:\/\/p\.fly\.dev\/foo\/view\/a\.B/);
});
