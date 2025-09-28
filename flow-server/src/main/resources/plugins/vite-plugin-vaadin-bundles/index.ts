/// <reference types="node" />
import { readFileSync } from 'node:fs';
import * as path from 'node:path';
import { Plugin, mergeConfig } from 'vite';
import { createRequire } from 'module';

// Make `require` compatible with ES modules
const require = createRequire(import.meta.url);

/**
 * Internal Vite plugin that enables @vaadin/bundles for development mode.
 *
 * @private
 */
export default function vaadinBundlesPlugin({ nodeModulesFolder }: {
  nodeModulesFolder: string;
}): Plugin {
  type ExportInfo =
    | string
    | {
        namespace?: string;
        source: string;
      };

  type ExposeInfo = {
    exports: ExportInfo[];
  };

  type PackageInfo = {
    version: string;
    exposes: Record<string, ExposeInfo>;
  };

  type BundleJson = {
    packages: Record<string, PackageInfo>;
  };

  const disabledMessage = 'Vaadin component dependency bundles are disabled.';

  const modulesDirectory = nodeModulesFolder.replace(/\\/g, '/');

  let vaadinBundleJson: BundleJson;

  function parseModuleId(id: string): { packageName: string; modulePath: string } {
    const [scope, scopedPackageName] = id.split('/', 3);
    const packageName = scope.startsWith('@') ? `${scope}/${scopedPackageName}` : scope;
    const modulePath = `.${id.substring(packageName.length)}`;
    return {
      packageName,
      modulePath
    };
  }

  function getExports(id: string): string[] | undefined {
    const { packageName, modulePath } = parseModuleId(id);
    const packageInfo = vaadinBundleJson.packages[packageName];

    if (!packageInfo) return;

    const exposeInfo: ExposeInfo = packageInfo.exposes[modulePath];
    if (!exposeInfo) return;

    const exportsSet = new Set<string>();
    for (const e of exposeInfo.exports) {
      if (typeof e === 'string') {
        exportsSet.add(e);
      } else {
        const { namespace, source } = e;
        if (namespace) {
          exportsSet.add(namespace);
        } else {
          const sourceExports = getExports(source);
          if (sourceExports) {
            sourceExports.forEach((e) => exportsSet.add(e));
          }
        }
      }
    }
    return Array.from(exportsSet);
  }

  function getExportBinding(binding: string) {
    return binding === 'default' ? '_default as default' : binding;
  }

  function getImportAssigment(binding: string) {
    return binding === 'default' ? 'default: _default' : binding;
  }

  return {
    name: 'vaadin:bundles',
    enforce: 'pre',
    apply(_config, { command }) {
      if (command !== 'serve') return false;

      try {
        const vaadinBundleJsonPath = require.resolve('@vaadin/bundles/vaadin-bundle.json');
        vaadinBundleJson = JSON.parse(readFileSync(vaadinBundleJsonPath, { encoding: 'utf8' }));
      } catch (e: unknown) {
        if (typeof e === 'object' && (e as { code: string }).code === 'MODULE_NOT_FOUND') {
          vaadinBundleJson = { packages: {} };
          console.info(`@vaadin/bundles npm package is not found, ${disabledMessage}`);
          return false;
        } else {
          throw e;
        }
      }

      const versionMismatches: Array<{ name: string; bundledVersion: string; installedVersion: string }> = [];
      for (const [name, packageInfo] of Object.entries(vaadinBundleJson.packages)) {
        let installedVersion: string | undefined = undefined;
        try {
          const { version: bundledVersion } = packageInfo;
          const installedPackageJsonFile = path.resolve(modulesDirectory, name, 'package.json');
          const packageJson = JSON.parse(readFileSync(installedPackageJsonFile, { encoding: 'utf8' }));
          installedVersion = packageJson.version;
          if (installedVersion && installedVersion !== bundledVersion) {
            versionMismatches.push({
              name,
              bundledVersion,
              installedVersion
            });
          }
        } catch (_) {
          // ignore package not found
        }
      }
      if (versionMismatches.length) {
        console.info(`@vaadin/bundles has version mismatches with installed packages, ${disabledMessage}`);
        console.info(`Packages with version mismatches: ${JSON.stringify(versionMismatches, undefined, 2)}`);
        vaadinBundleJson = { packages: {} };
        return false;
      }

      return true;
    },
    async config(config) {
      return mergeConfig(
        {
          optimizeDeps: {
            exclude: [
              // Vaadin bundle
              '@vaadin/bundles',
              ...Object.keys(vaadinBundleJson.packages)
            ]
          }
        },
        config
      );
    },
    load(rawId) {
      const [path, params] = rawId.split('?');
      if (!path.startsWith(modulesDirectory)) return;

      const id = path.substring(modulesDirectory.length + 1);
      const bindings = getExports(id);
      if (bindings === undefined) return;

      const cacheSuffix = params ? `?${params}` : '';
      const bundlePath = `@vaadin/bundles/vaadin.js${cacheSuffix}`;

      return `import { init as VaadinBundleInit, get as VaadinBundleGet } from '${bundlePath}';
await VaadinBundleInit('default');
const { ${bindings.map(getImportAssigment).join(', ')} } = (await VaadinBundleGet('./node_modules/${id}'))();
export { ${bindings.map(getExportBinding).join(', ')} };`;
    }
  };
}
