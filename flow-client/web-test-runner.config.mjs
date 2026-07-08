import { esbuildPlugin } from '@web/dev-server-esbuild';
import { fileURLToPath } from 'url';

export default {
  // Test files end in Tests.ts; other .ts under src/test/frontend are shared
  // helpers/fixtures, not test files.
  files: ['src/test/frontend/*Tests.ts'],
  // The tests import components that pull in Lit, whose development build logs a
  // "Lit is in dev mode" banner when it loads. Pre-mark that warning as issued in
  // an inline script that runs before any module (and thus before Lit) loads, so
  // the banner stays out of the test output.
  testRunnerHtml: (testFramework) => `
    <!DOCTYPE html>
    <html>
      <head>
        <script>
          (globalThis.litIssuedWarnings ??= new Set()).add('dev-mode');
        </script>
      </head>
      <body>
        <script type="module" src="${testFramework}"></script>
      </body>
    </html>`,
  plugins: [
    esbuildPlugin({
      ts: true,
      tsconfig: fileURLToPath(new URL('./tsconfig.json', import.meta.url))
    })
  ]
};
