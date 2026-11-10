import { PluginOption, UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated.ts';

function addCssToIndex(): PluginOption {
  return {
    name: 'generate-css',
    transformIndexHtml: (_html, _conf) => {
      const tags = [
        {
          tag: 'link',
          attrs: {
            rel: 'stylesheet',
            href: '/imported-by-vite-plugin.css'
          }
        }
      ];

      return tags;
    }
  };
}

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  plugins: [addCssToIndex()],
  build: {
    // Emit separate .map files for the dev bundle so that
    // DevBundleSourceMapsIT can verify that the build plugins keep them usable
    sourcemap: true
  }
});

export default overrideVaadinConfig(customConfig);
