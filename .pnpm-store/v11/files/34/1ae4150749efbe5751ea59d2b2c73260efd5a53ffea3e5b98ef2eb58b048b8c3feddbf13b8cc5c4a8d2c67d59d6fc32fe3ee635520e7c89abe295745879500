import * as babel from "@babel/core";
import { GeneralHookFilter, ModuleTypeFilter, Plugin } from "rolldown";
import { Plugin as Plugin$1, ResolvedConfig } from "vite";

//#region src/babelCompat.d.ts
type IsAny<T> = boolean extends (T extends never ? true : false) ? true : false;
/** **When using babel 7, install `@types/babel__core` to get proper types.** */
type InputOptionsFallback = {
  /** **When using babel 7, install `@types/babel__core` to get proper types.** */plugins?: unknown[]; /** **When using babel 7, install `@types/babel__core` to get proper types.** */
  presets?: unknown[];
};
type InputOptions8 = babel.InputOptions;
type PresetItem8 = babel.PresetItem;
type TransformOptions = babel.TransformOptions;
type InputOptions = IsAny<InputOptions8> extends false ? InputOptions8 : IsAny<TransformOptions> extends false ? TransformOptions : InputOptionsFallback;
type PresetItem = IsAny<PresetItem8> extends false ? PresetItem8 : babel.PluginItem;
//#endregion
//#region src/rolldownPreset.d.ts
type PartialEnvironment = Parameters<NonNullable<Plugin$1['applyToEnvironment']>>[0];
type RolldownBabelPreset = {
  preset: PresetItem;
  rolldown: {
    filter?: {
      id?: GeneralHookFilter;
      moduleType?: ModuleTypeFilter;
      code?: GeneralHookFilter;
    };
    optimizeDeps?: {
      include?: string[];
    };
    applyToEnvironmentHook?: (environment: PartialEnvironment) => boolean;
    configResolvedHook?: (config: ResolvedConfig) => boolean;
  };
};
type RolldownBabelPresetItem = PresetItem | RolldownBabelPreset;
declare function defineRolldownBabelPreset(preset: RolldownBabelPreset): RolldownBabelPreset;
//#endregion
//#region src/options.d.ts
interface InnerTransformOptions extends Partial<Pick<InputOptions, 'assumptions' | 'auxiliaryCommentAfter' | 'auxiliaryCommentBefore' | 'exclude' | 'comments' | 'compact' | 'cwd' | 'generatorOpts' | 'include' | 'parserOpts' | 'plugins' | 'retainLines' | 'shouldPrintComment' | 'targets' | 'wrapPluginVisitorMethod'>> {
  /**
   * List of presets (a set of plugins) to load and use
   *
   * Default: `[]`
   */
  presets?: RolldownBabelPresetItem[] | undefined;
}
interface PluginOptions extends Omit<InnerTransformOptions, 'include' | 'exclude'> {
  /**
   * When set, automatically adds `@babel/plugin-transform-runtime` so that
   * babel helpers are imported from `@babel/runtime` instead of being inlined
   * into every file.
   *
   * Requires `@babel/plugin-transform-runtime` and `@babel/runtime` to be installed.
   */
  runtimeVersion?: string;
  /**
   * If specified, only files matching the pattern will be processed by babel.
   * @default `/\.(?:[jt]sx?|[cm][jt]s)(?:$|\?)/`
   *
   * Note that this option receives the syntax supported by babel instead of picomatch.
   * @see https://babeljs.io/docs/options#matchpattern
   */
  include?: InnerTransformOptions['include'];
  /**
   * If any of patterns match, babel will not process the file.
   * @default `/[\/\\]node_modules[\/\\]/`
   *
   * Note that this option receives the syntax supported by babel instead of picomatch.
   * @see https://babeljs.io/docs/options#matchpattern
   */
  exclude?: InnerTransformOptions['exclude'];
  /**
   * If false, skips source map generation. This will improve performance.
   * @default true
   */
  sourceMap?: boolean;
  /**
   * Allows users to provide an array of options that will be merged into the current configuration one at a time.
   * This feature is best used alongside the "test"/"include"/"exclude" options to provide conditions for which an override should apply
   */
  overrides?: InnerTransformOptions[] | undefined;
}
//#endregion
//#region src/index.d.ts
declare function babelPlugin(rawOptions: PluginOptions): Promise<Plugin>;
//#endregion
export { type RolldownBabelPreset, babelPlugin as default, defineRolldownBabelPreset };