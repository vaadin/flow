import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  server: {
    fs: {
      allow: ['../package-outside-npm', '../package2-outside-npm']
    }
  }
});

export default overrideVaadinConfig(customConfig);
