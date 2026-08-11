import { esbuildPlugin } from '@web/dev-server-esbuild';
import { defaultReporter } from '@web/test-runner';
import { junitReporter } from '@web/test-runner-junit-reporter';
import { fileURLToPath } from 'url';

export default {
  files: ['src/test/frontend/*.ts'],
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
