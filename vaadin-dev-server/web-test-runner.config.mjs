import { esbuildPlugin } from '@web/dev-server-esbuild';
import { fileURLToPath } from "url";

export default {
  files: ['src/main/resources/META-INF/frontend/vaadin-dev-tools/**/*.test.ts'],
  plugins: [
    esbuildPlugin({
      ts: true,
      tsconfig: fileURLToPath(new URL('./tsconfig.json', import.meta.url)),
    }),
  ],
};
