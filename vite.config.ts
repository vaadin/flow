import path from 'path';
import { PluginOption, UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

function useLocalWebComponents(webComponentsDir: string): PluginOption {
  const webComponentsNodeModulesPath = path.resolve(__dirname, webComponentsDir, 'node_modules');
  return {
    name: 'vaadin:use-local-web-components',
    enforce: 'pre',
    config(config) {
      config.server = config.server ?? {};
      config.server.fs = config.server.fs ?? {};
      config.server.fs.allow = config.server.fs.allow ?? [];
      config.server.fs.allow.push(webComponentsNodeModulesPath);
      config.server.watch = config.server.watch ?? {};
      config.server.watch.ignored = [`!${webComponentsNodeModulesPath}/**`];
    },
    resolveId(id) {
      if (/^(@polymer|@vaadin)/.test(id)) {
        return this.resolve(path.join(webComponentsNodeModulesPath, id));
      }
    },
  };
}

const customConfig: UserConfigFn = () => ({
  optimizeDeps: {
    exclude: ['@vaadin', '@polymer'],
  },
  plugins: [useLocalWebComponents('web-components')],
});

export default overrideVaadinConfig(customConfig);
