import { esbuildPlugin } from '@web/dev-server-esbuild';
import { fileURLToPath } from 'url';

export default {
  files: ['src/test/frontend/*.ts'],
  plugins: [
    esbuildPlugin({
      ts: true,
      // esbuild does not read 'target' from tsconfig and defaults to esnext,
      // which leaves standard decorators and accessor fields untransformed and
      // unparseable by the browser. Set it explicitly to match tsconfig so the
      // ConnectionIndicator Lit element is lowered for the test browser.
      target: 'es2023',
      tsconfig: fileURLToPath(new URL('./tsconfig.json', import.meta.url))
    })
  ]
};
