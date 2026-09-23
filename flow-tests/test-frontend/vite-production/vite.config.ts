import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated.ts';

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  build: {
    // Emit separate .map files for the production bundle so that
    // SourceMapsIT can verify that the build plugins keep them usable
    sourcemap: true
  }
});

export default overrideVaadinConfig(customConfig);
