import { build } from 'vite';
import { getManifest } from 'workbox-build';
import brotli from 'rollup-plugin-brotli';

const APP_SHELL_URL = '.';

function injectManifestToSWPlugin({ outDir }) {
  const rewriteManifestIndexHtmlUrl = (manifest) => {
    const indexEntry = manifest.find((entry) => entry.url === 'index.html');
    if (indexEntry) {
      indexEntry.url = APP_SHELL_URL;
    }

    return { manifest, warnings: [] };
  };

  return {
    name: 'vaadin:inject-manifest-to-sw',
    async transform(code, id) {
      if (/sw\.(ts|js)$/.test(id)) {
        const { manifestEntries } = await getManifest({
          globDirectory: outDir,
          globPatterns: ['**/*'],
          globIgnores: ['**/*.br', 'pwa-icons/**'],
          manifestTransforms: [rewriteManifestIndexHtmlUrl],
          maximumFileSizeToCacheInBytes: 100 * 1024 * 1024 // 100mb,
        });

        return code.replace('self.__WB_MANIFEST', JSON.stringify(manifestEntries));
      }
    }
  };
}

/**
 * Internal Vite plugin that builds the service worker. Not intended for public use.
 *
 * @private
 */
export default function serviceWorkerPlugin({ srcPath }) {
  let buildConfig;
  let buildOutput;

  return {
    name: 'vaadin:build-sw',
    enforce: 'post',
    async configResolved(viteConfig) {
      buildConfig = {
        base: viteConfig.base,
        root: viteConfig.root,
        mode: viteConfig.mode,
        resolve: viteConfig.resolve,
        define: {
          ...viteConfig.define,
          'process.env.NODE_ENV': JSON.stringify(viteConfig.mode),
        },
        build: {
          write: viteConfig.mode !== 'development',
          minify: viteConfig.build.minify,
          outDir: viteConfig.build.outDir,
          sourcemap: viteConfig.command === 'serve' || viteConfig.build.sourcemap,
          emptyOutDir: false,
          modulePreload: false,
          rollupOptions: {
            input: {
              sw: srcPath
            },
            output: {
              exports: 'none',
              entryFileNames: 'sw.js',
              inlineDynamicImports: true,
            },
          },
        },
      };
    },
    async buildStart() {
      if (buildConfig.mode === 'development') {
        buildOutput = await build(buildConfig);
      }
    },
    async load(id) {
      if (id.endsWith('sw.js')) {
        return buildOutput.output[0].code;
      }
    },
    async closeBundle() {
      if (buildConfig.mode !== 'development') {
        await build({
          ...buildConfig,
          plugins: [injectManifestToSWPlugin({ outDir: buildConfig.build.outDir }), brotli()]
        });
      }
    },
  };
}
