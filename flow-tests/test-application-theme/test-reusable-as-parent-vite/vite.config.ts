import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  build: {
    assetsInlineLimit: 0
  }
});

export default overrideVaadinConfig(customConfig);
