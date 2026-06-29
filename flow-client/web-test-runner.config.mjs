import { esbuildPlugin } from '@web/dev-server-esbuild';
import { fileURLToPath } from 'url';

export default {
  // Test files end in Tests.ts; other .ts here are helpers/entries (e.g.
  // gwtTestInternals.ts, bundled separately for GwtTests).
  files: ['src/test/frontend/*Tests.ts'],
  plugins: [
    esbuildPlugin({
      ts: true,
      tsconfig: fileURLToPath(new URL('./tsconfig.json', import.meta.url))
    })
  ]
};
