import { esbuildPlugin } from '@web/dev-server-esbuild';
import { defaultReporter } from '@web/test-runner';
import { junitReporter } from '@web/test-runner-junit-reporter';
import { fileURLToPath } from 'url';

export default {
  // Test files end in Tests.ts; anything else under src/test/frontend is a
  // shared helper or fixture rather than a suite.
  files: ['src/test/frontend/**/*Tests.ts'],
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
  ],
  reporters: [
    // Keep the human-readable console output.
    defaultReporter(),
    // Emit a JUnit XML report so the frontend test results are picked up by
    // the same reporting as the Java tests. The file is written into a
    // *-reports directory named TEST*.xml so it matches both the CI artifact
    // collection (find ... -name surefire-reports) and the test-results job's
    // junit_files glob (**/target/*-reports/TEST*.xml).
    junitReporter({
      outputPath: './target/surefire-reports/TEST-flow-client-frontend.xml',
      reportLogs: true
    })
  ]
};
