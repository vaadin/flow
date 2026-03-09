import path from 'path';

// Dependencies imported both in the external web-components and in Flow application sources.
// Vite resolves dependencies from where they are imported, so these would be bundled twice.
// To avoid that, use dedupe. Dedupe only works for non-optimized dependencies, so they also
// need to be excluded from optimization / pre-bundling.
const sharedDeps = ['lit', 'lit-html', 'ol', '@polymer/polymer'];

export default function useLocalWebComponents(webComponentsDir = 'web-components') {
  const nodeModulesPath = path.resolve(webComponentsDir, 'node_modules');
  return {
    name: 'vaadin:use-local-web-components',
    enforce: 'pre',
    config() {
      console.info('Using local web components from ' + webComponentsDir);
      return {
        server: {
          fs: {
            allow: [nodeModulesPath]
          },
          watch: {
            ignored: [`!${nodeModulesPath}/**`]
          }
        },
        optimizeDeps: {
          exclude: ['@vaadin', ...sharedDeps]
        },
        resolve: {
          dedupe: sharedDeps
        }
      };
    },
    resolveId(id) {
      if (/^@vaadin/.test(id)) {
        return this.resolve(path.join(nodeModulesPath, id));
      }
    }
  };
}
