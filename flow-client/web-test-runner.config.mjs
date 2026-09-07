import { esbuildPlugin } from '@web/dev-server-esbuild';
import { defaultReporter } from '@web/test-runner';
import { junitReporter } from '@web/test-runner-junit-reporter';
import { fileURLToPath } from 'url';

export default {
  // Test files end in Tests.ts; anything else under src/test/frontend is a
  // shared helper or fixture rather than a suite. Suites are discovered
  // recursively so tests can mirror the ported modules' directory layout (e.g.
  // src/test/frontend/internal/client/flow/reactive/ComputationTests.ts).
  files: ['src/test/frontend/**/*Tests.ts'],
  // Mocha allows a case 2 seconds by default. The route-action case in FlowTests
  // awaits the engine's lazy dynamic import, which esbuild compiles on demand:
  // that measures around one second on an idle machine and has crossed two on a
  // loaded CI runner, failing the case on timing alone. Give every case room
  // instead of letting the runner's load decide.
  testFramework: {
    config: {
      timeout: '10000'
    }
  },
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
    {
      // Flow.ts loads the client through the bare `vaadin-flow-client`
      // specifier, which an application's generated Vite config aliases to
      // jar-resources. Point it at the sources so the dynamic import resolves
      // when the tests run in the browser.
      name: 'vaadin-flow-client-resolver',
      resolveImport({ source }) {
        return source === 'vaadin-flow-client' ? '/src/main/frontend/FlowClient.js' : undefined;
      }
    },
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
