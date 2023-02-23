import { esbuildPlugin } from '@web/dev-server-esbuild';

export default {
  files: ['src/main/resources/META-INF/frontend/vaadin-dev-tools/**/*.test.ts'],
  plugins: [esbuildPlugin({ ts: true })],
};
