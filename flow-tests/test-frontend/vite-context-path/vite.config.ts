import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

/**
 * Dumps effective contents of config.optimizeDeps for tests
 */
function dumpOptimizeDepsPlugin(): PluginOption {
  let config

  return {
    name: 'dump-optimize-deps',
    configResolved(_config) {
      config = _config;
    },
    transformIndexHtml: html => {
      return html.replace('</head>', `  <script>
    window.ViteConfigOptimizeDeps = ${JSON.stringify(config.optimizeDeps)};
  </script>
</head>`);
    }
  }
}

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
  plugins: [
    dumpOptimizeDepsPlugin()
  ]
});

export default overrideVaadinConfig(customConfig);
