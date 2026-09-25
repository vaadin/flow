# @rolldown/plugin-babel [![npm](https://img.shields.io/npm/v/@rolldown/plugin-babel.svg)](https://npmx.dev/package/@rolldown/plugin-babel)

Rolldown plugin for transforming code with [Babel](https://babeljs.io/).

## Install

```bash
pnpm add -D @rolldown/plugin-babel @babel/core
```

If you are using TypeScript, you will also need to install `@types/babel__core`:

```bash
pnpm add -D @types/babel__core
```

## Usage

```js
import babel from '@rolldown/plugin-babel'

export default {
  plugins: [
    babel({
      plugins: ['@babel/plugin-proposal-throw-expressions'],
    }),
  ],
}
```

The plugin automatically configures Babel's parser for `.jsx`, `.ts`, and `.tsx` files.

> **Note:** This plugin does not load Babel configuration files (e.g., `babel.config.js`, `.babelrc`). All Babel options must be passed directly through the plugin options.

## Options

### `include`

- **Type:** `string | RegExp | (string | RegExp)[]`
- **Default:** `/\.(?:[jt]sx?|[cm][jt]s)(?:$|\?)/`

Only files matching the pattern will be processed.

Note that this option receives [the syntax supported by babel](https://babeljs.io/docs/options#matchpattern) instead of picomatch.

### `exclude`

- **Type:** `string | RegExp | (string | RegExp)[]`
- **Default:** `/[\/\\]node_modules[\/\\]|\0rolldown\/runtime\.js/`

Files matching the pattern will be skipped. The default also excludes Rolldown's runtime helper module (`\0rolldown/runtime.js`) so that Babel does not transform Rolldown's internal runtime code. If you override `exclude`, you are responsible for re-adding any of these entries you still want to skip.

Note that this option receives [the syntax supported by babel](https://babeljs.io/docs/options#matchpattern) instead of picomatch.

### `sourceMap`

- **Type:** `boolean`
- **Default:** `true`

Set to `false` to skip source map generation for better performance.

### `presets`

- **Type:** `(babel.PresetItem | RolldownBabelPreset)[]`

List of Babel presets to apply. Supports both standard Babel presets and Rolldown-enhanced presets with per-file filtering (see [Rolldown Babel Presets](#rolldown-babel-presets)).

### `plugins`

- **Type:** `babel.PluginItem[]`

List of Babel plugins to apply.

### `overrides`

- **Type:** `InnerTransformOptions[]`

Array of additional configurations that are merged into the current configuration. Use with Babel's `test`/`include`/`exclude` options to conditionally apply overrides.

### `runtimeVersion`

- **Type:** `string`

When set, automatically adds [`@babel/plugin-transform-runtime`](https://babeljs.io/docs/babel-plugin-transform-runtime) so that Babel helpers are imported from `@babel/runtime` instead of being inlined into every file. This deduplicates helpers across modules and reduces bundle size.

The value is the version of `@babel/runtime` that is assumed to be installed. If you are externalizing `@babel/runtime` (for example, you are packaging a library), you should set the version range of `@babel/runtime` in your package.json. If you are bundling `@babel/runtime` for your application, you should set the version of `@babel/runtime` that is installed.

```bash
pnpm add -D @babel/plugin-transform-runtime @babel/runtime
```

```js
import babel from '@rolldown/plugin-babel'

// if you are externalizing @babel/runtime
import fs from 'node:fs'
import path from 'node:path'
const packageJson = JSON.parse(
  fs.readFileSync(path.join(import.meta.dirname, 'package.json'), 'utf8'),
)
const babelRuntimeVersion = packageJson.dependencies['@babel/runtime']

// if you are bundling @babel/runtime
import babelRuntimePackageJson from '@babel/runtime/package.json'
const babelRuntimeVersion = babelRuntimePackageJson.version

export default {
  plugins: [
    babel({
      runtimeVersion: babelRuntimeVersion,
      plugins: ['@babel/plugin-proposal-decorators'],
    }),
  ],
}
```

### Other Babel options

The following [Babel options](https://babeljs.io/docs/options) are forwarded directly:

`assumptions`, `auxiliaryCommentAfter`, `auxiliaryCommentBefore`, `comments`, `compact`, `cwd`, `generatorOpts`, `parserOpts`, `retainLines`, `shouldPrintComment`, `targets`, `wrapPluginVisitorMethod`

## Rolldown Babel Presets

Standard Babel presets are applied to every file. When a preset should only apply to certain files, wrap it in a `RolldownBabelPreset` with a `filter`. Use the `defineRolldownBabelPreset` helper for type checking:

```js
import babelPlugin, { defineRolldownBabelPreset } from '@rolldown/plugin-babel'

const myReactPreset = defineRolldownBabelPreset({
  preset: ['@babel/preset-react'],
  rolldown: {
    filter: {
      id: /\.tsx?$/,
      moduleType: ['tsx', 'jsx'],
      code: /from ['"]react['"]/,
    },
  },
})

export default {
  plugins: [
    babelPlugin({
      presets: [myReactPreset],
    }),
  ],
}
```

### Filter dimensions

All filter dimensions are optional. When multiple dimensions are specified, all must match for the preset to apply.

#### `id`

Match files by path. Accepts a string glob, RegExp, array, or `{ include, exclude }` object.

```js
{ id: /\.tsx$/ }
{ id: '**/*.tsx' }
{ id: [/\.tsx$/, /\.jsx$/] }
{ id: { include: [/\.tsx$/], exclude: [/test\.tsx$/] } }
```

#### `moduleType`

Match by Rolldown module type. Accepts a string array or `{ include }` object.

```js
{
  moduleType: ['tsx', 'jsx']
}
{
  moduleType: {
    include: ['tsx']
  }
}
```

#### `code`

Match by file content. Accepts a RegExp, array, or `{ include, exclude }` object.

```js
{ code: /import React/ }
{ code: { include: [/import React/], exclude: [/\/\/ @no-transform/] } }
```

### `configResolvedHook`

When used with Vite, a preset can define a `configResolvedHook` callback to conditionally enable or disable itself based on the resolved Vite config, similar to [`configResolved` hook](https://vite.dev/guide/api-plugin#configresolved). The callback receives the `ResolvedConfig` and should return `false` to remove the preset.

```js
defineRolldownBabelPreset({
  preset: ['@babel/preset-react'],
  rolldown: {
    filter: { id: /\.[jt]sx$/ },
    configResolvedHook(config) {
      // Only apply during production builds
      return config.command === 'build'
    },
  },
})
```

When running without Vite (pure Rolldown), `configResolvedHook` is ignored.

### `applyToEnvironmentHook`

When used with Vite, a preset can define an `applyToEnvironmentHook` callback to conditionally enable or disable itself based on the Vite environment, similar to [`applyToEnvironment` hook](https://vite.dev/guide/api-environment-plugins#per-environment-plugins). The callback receives the `PartialEnvironment` and should return `false` to remove the preset for that environment.

```js
defineRolldownBabelPreset({
  preset: ['@babel/preset-react'],
  rolldown: {
    filter: { id: /\.[jt]sx$/ },
    applyToEnvironmentHook(environment) {
      // Only apply in the client environment
      return environment.name === 'client'
    },
  },
})
```

When running without Vite (pure Rolldown), `applyToEnvironmentHook` is ignored.

### `optimizeDeps`

A preset can declare dependencies that should be pre-bundled by Vite's dependency optimizer. The plugin automatically merges these into [`optimizeDeps.include`](https://vite.dev/config/dep-optimization-options#optimizedeps-include) in the Vite config.

```js
defineRolldownBabelPreset({
  preset: ['@babel/preset-react'],
  rolldown: {
    filter: { id: /\.[jt]sx$/ },
    optimizeDeps: {
      include: ['react', 'react-dom'],
    },
  },
})
```

When running without Vite (pure Rolldown), `optimizeDeps` is ignored.

### How preset filters work

Preset filters operate at two levels:

1. **Per-file filtering** — When Rolldown calls the transform hook, each preset's filter is checked against the current file. Presets whose filter doesn't match are skipped for that file.

2. **Transform hook pre-filtering** — The plugin computes a union of all preset filters to tell Rolldown which files to send to the transform hook in the first place. This avoids calling into the plugin for files that no preset would match.

You can mix standard Babel presets and Rolldown presets freely:

```js
babelPlugin({
  presets: [
    '@babel/preset-env', // applied to all files
    {
      preset: ['@babel/preset-react'],
      rolldown: { filter: { id: /\.[jt]sx$/ } },
    },
  ],
})
```

## License

MIT
