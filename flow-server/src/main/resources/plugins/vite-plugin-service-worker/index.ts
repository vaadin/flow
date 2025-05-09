/// <reference types="node" />
import { resolve } from 'node:path';
import type { RollupOutput } from 'rollup';
import { build, InlineConfig, Plugin } from 'vite';
import { getManifest, ManifestTransform } from 'workbox-build';
import brotli from 'rollup-plugin-brotli';

const APP_SHELL_URL = '.';

function injectManifestToSWPlugin({ outDir }: { outDir: string }): Plugin {
  const rewriteManifestIndexHtmlUrl: ManifestTransform = (manifest) => {
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

      return;
    }
  };
}

/**
 * Internal Vite plugin that builds the service worker. Not intended for public use.
 *
 * @private
 */
export default function serviceWorkerPlugin({ srcPath }: { srcPath: string }): Plugin {
  let buildConfig: InlineConfig;
  let buildOutput: RollupOutput;
  let swSourcePath = resolve(srcPath);

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
          'globalThis.document': undefined,
        },
        build: {
          write: viteConfig.mode !== 'development',
          minify: viteConfig.build.minify,
          outDir: viteConfig.build.outDir,
          target: viteConfig.build.target,
          sourcemap: viteConfig.command === 'serve' || viteConfig.build.sourcemap,
          emptyOutDir: false,
          modulePreload: false,
          rollupOptions: {
            input: {
              sw: swSourcePath,
            },
            output: {
              format: 'iife',
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
        buildOutput = await build(buildConfig) as RollupOutput;
      }
    },
    resolveId(id) {
      if (id === '/sw.js') {
        return swSourcePath;
      }

      return;
    },
    async load(id) {
      if (id === swSourcePath) {
        return buildOutput.output[0].code;
      }

      return;
    },
    async closeBundle() {
      if (buildConfig.mode !== 'development') {
        await build({
          ...buildConfig,
          plugins: [injectManifestToSWPlugin({ outDir: buildConfig.build!.outDir! }), brotli()]
        });
      }
    },
  };
}
