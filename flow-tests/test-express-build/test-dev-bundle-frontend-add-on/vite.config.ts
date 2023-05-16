import { PluginOption, UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

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
  plugins: [addCssToIndex()]
});

export default overrideVaadinConfig(customConfig);
