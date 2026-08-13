import picomatch from "picomatch";
import * as babel from "@babel/core";
//#region \0rolldown/runtime.js
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __exportAll = (all, no_symbols) => {
	let target = {};
	for (var name in all) __defProp(target, name, {
		get: all[name],
		enumerable: true
	});
	if (!no_symbols) __defProp(target, Symbol.toStringTag, { value: "Module" });
	return target;
};
var __copyProps = (to, from, except, desc) => {
	if (from && typeof from === "object" || typeof from === "function") for (var keys = __getOwnPropNames(from), i = 0, n = keys.length, key; i < n; i++) {
		key = keys[i];
		if (!__hasOwnProp.call(to, key) && key !== except) __defProp(to, key, {
			get: ((k) => from[k]).bind(null, key),
			enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable
		});
	}
	return to;
};
var __reExport = (target, mod, secondTarget) => (__copyProps(target, mod, "default"), secondTarget && __copyProps(secondTarget, mod, "default"));
//#endregion
//#region src/utils.ts
function arrayify(value) {
	return Array.isArray(value) ? value : [value];
}
function filterMap(array, predicate) {
	const newArray = [];
	for (const [index, item] of array.entries()) {
		const result = predicate(item, index);
		if (result) newArray.push(result.value);
	}
	return newArray;
}
//#endregion
//#region src/rolldownPreset.ts
function compilePattern(pattern, isPath) {
	if (pattern instanceof RegExp) return (value) => pattern.test(value);
	if (isPath) return picomatch(pattern);
	return (value) => value.includes(pattern);
}
function compilePatterns(patterns, isPath) {
	const matchers = patterns.map((p) => compilePattern(p, isPath));
	return (value) => matchers.some((m) => m(value));
}
/**
* Pre-compile a GeneralHookFilter into a single matcher function.
* Returns undefined when the filter matches everything.
*/
function compileGeneralHookFilter(filter, isPath) {
	if (filter == null) return void 0;
	let include;
	let exclude;
	if (typeof filter === "string" || filter instanceof RegExp) include = [filter];
	else if (Array.isArray(filter)) include = filter;
	else {
		include = filter.include != null ? arrayify(filter.include) : void 0;
		exclude = filter.exclude != null ? arrayify(filter.exclude) : void 0;
	}
	const includeMatcher = include ? compilePatterns(include, isPath) : void 0;
	const excludeMatcher = exclude ? compilePatterns(exclude, isPath) : void 0;
	if (includeMatcher && excludeMatcher) return (value) => !excludeMatcher(value) && includeMatcher(value);
	if (excludeMatcher) return (value) => !excludeMatcher(value);
	return includeMatcher;
}
function compileModuleTypeFilter(filter) {
	if (filter == null) return void 0;
	const types = Array.isArray(filter) ? filter : filter.include ?? [];
	if (types.length === 0) return void 0;
	const typeSet = new Set(types);
	return (value) => typeSet.has(value);
}
/**
* Pre-compile a preset's filter into a single matcher function
* that checks all dimensions (id, moduleType, code) at once.
* Returns undefined when the filter matches everything.
*/
function compilePresetFilter(filter) {
	if (!filter) return void 0;
	const matchId = compileGeneralHookFilter(filter.id, true);
	const matchModuleType = compileModuleTypeFilter(filter.moduleType);
	const matchCode = compileGeneralHookFilter(filter.code, false);
	if (!matchId && !matchModuleType && !matchCode) return void 0;
	return (ctx) => (!matchId || matchId(ctx.id)) && (!matchModuleType || matchModuleType(ctx.moduleType)) && (!matchCode || matchCode(ctx.code));
}
function defineRolldownBabelPreset(preset) {
	return preset;
}
function convertToBabelPresetItem(ctx, preset, compiledFilter) {
	if (typeof preset !== "object" || !("rolldown" in preset)) return { value: preset };
	if (compiledFilter && !compiledFilter(ctx)) return void 0;
	return { value: preset.preset };
}
//#endregion
//#region src/options.ts
const DEFAULT_INCLUDE = [/\.(?:[jt]sx?|[cm][jt]s)(?:$|\?)/];
const DEFAULT_EXCLUDE = [/[/\\]node_modules[/\\]|^\0rolldown\/runtime\.js$/];
function resolveOptions(options) {
	return {
		...options,
		include: options.include ?? DEFAULT_INCLUDE,
		exclude: options.exclude ?? DEFAULT_EXCLUDE,
		sourceMap: options.sourceMap ?? true
	};
}
function compilePresetFilters(presets) {
	return presets.map((preset) => typeof preset === "object" && "rolldown" in preset ? compilePresetFilter(preset.rolldown.filter) : void 0);
}
function filterPresetArrayWithEnvironment(presets, environment) {
	return presets.filter((preset) => {
		if (typeof preset !== "object" || !("rolldown" in preset)) return true;
		if (!preset.rolldown.applyToEnvironmentHook) return true;
		return preset.rolldown.applyToEnvironmentHook(environment);
	});
}
function filterPresetsWithEnvironment(options, environment) {
	return {
		...options,
		presets: options.presets ? filterPresetArrayWithEnvironment(options.presets, environment) : void 0,
		overrides: options.overrides?.map((override) => override.presets ? {
			...override,
			presets: filterPresetArrayWithEnvironment(override.presets, environment)
		} : override)
	};
}
function filterPresetArray(presets, config) {
	return presets.filter((preset) => {
		if (typeof preset !== "object" || !("rolldown" in preset)) return true;
		if (!preset.rolldown.configResolvedHook) return true;
		return preset.rolldown.configResolvedHook(config);
	});
}
function filterPresetsWithConfigResolved(options, config) {
	return {
		...options,
		presets: options.presets ? filterPresetArray(options.presets, config) : void 0,
		overrides: options.overrides?.map((override) => override.presets ? {
			...override,
			presets: filterPresetArray(override.presets, config)
		} : override)
	};
}
function collectOptimizeDepsInclude(options) {
	const result = [];
	for (const preset of options.presets ?? []) if (typeof preset === "object" && "rolldown" in preset) result.push(...preset.rolldown.optimizeDeps?.include ?? []);
	for (const override of options.overrides ?? []) for (const preset of override.presets ?? []) if (typeof preset === "object" && "rolldown" in preset) result.push(...preset.rolldown.optimizeDeps?.include ?? []);
	return result;
}
/**
* Pre-compile all preset filters and return a function that
* converts options to babel options for a given context.
*/
function createBabelOptionsConverter(options) {
	const presetFilters = options.presets ? compilePresetFilters(options.presets) : void 0;
	const overridePresetFilters = options.overrides?.map((override) => override.presets ? compilePresetFilters(override.presets) : void 0);
	return function(ctx) {
		const { runtimeVersion: _, ...babelOptions } = options;
		return {
			...babelOptions,
			presets: options.presets ? filterMap(options.presets, (preset, i) => convertToBabelPresetItem(ctx, preset, presetFilters[i])) : void 0,
			overrides: options.overrides?.map((override, i) => override.presets ? {
				...override,
				presets: filterMap(override.presets, (preset, j) => convertToBabelPresetItem(ctx, preset, overridePresetFilters[i][j]))
			} : override)
		};
	};
}
//#endregion
//#region src/babelCompat.ts
var babelCompat_exports = /* @__PURE__ */ __exportAll({ loadOptionsAsync: () => loadOptionsAsync });
import * as import__babel_core from "@babel/core";
__reExport(babelCompat_exports, import__babel_core);
const loadOptionsAsync = babel.loadOptionsAsync;
//#endregion
//#region src/filter.ts
/**
* Extract string/RegExp values from babel's ConfigApplicableTest,
* filtering out function entries which HookFilter can't represent.
* If any function entry is present, returns undefined because the
* function could match anything we can't predict at the HookFilter level.
*/
function extractStringOrRegExp(test) {
	if (test === void 0) return void 0;
	const items = arrayify(test);
	const result = [];
	for (const item of items) {
		if (typeof item === "function") return;
		result.push(item);
	}
	return result.length > 0 ? result : void 0;
}
/**
* Normalize a GeneralHookFilter into { include?, exclude? } form.
*/
function normalizeGeneralHookFilter(filter) {
	if (filter == null) return {};
	if (typeof filter === "string" || filter instanceof RegExp) return { include: [filter] };
	if (Array.isArray(filter)) return { include: filter };
	return {
		include: filter.include != null ? arrayify(filter.include) : void 0,
		exclude: filter.exclude != null ? arrayify(filter.exclude) : void 0
	};
}
function isRolldownBabelPreset(preset) {
	return typeof preset === "object" && preset !== null && "rolldown" in preset;
}
function normalizeModuleTypeFilter(filter) {
	if (Array.isArray(filter)) return filter;
	return filter.include ?? [];
}
function patternKey(pattern) {
	return typeof pattern === "string" ? `s:${pattern}` : `r:${pattern.source}:${pattern.flags}`;
}
/**
* Compute the intersection of arrays by key.
* An item is kept only if it appears in every array.
* If any array is undefined, the intersection is empty.
*/
function intersectArrays(arrays, keyFn) {
	if (arrays.length === 0) return [];
	const defined = arrays.filter((a) => a != null);
	if (defined.length < arrays.length) return [];
	let result = new Map(defined[0].map((p) => [keyFn(p), p]));
	for (let i = 1; i < defined.length; i++) {
		const keys = new Set(defined[i].map((p) => keyFn(p)));
		for (const key of result.keys()) if (!keys.has(key)) result.delete(key);
	}
	return [...result.values()];
}
function concatArrays(a, b) {
	if (!a) return b;
	if (!b) return a;
	return [...a, ...b];
}
/**
* Union filter values from multiple presets for a single dimension.
* Includes are unioned (OR). Excludes are intersected (only items in ALL presets are kept).
*
* @param rawFilters  Per-preset filter values. undefined = no filter (matches everything).
* @param normalize   Converts a raw filter into { include?, exclude? } arrays.
* @param keyFn       Serializes an item for intersection comparison.
*/
function unionFilters(rawFilters, normalize, keyFn) {
	let matchAll = false;
	const includes = [];
	const excludeArrays = [];
	for (const raw of rawFilters) {
		if (raw === void 0) {
			matchAll = true;
			excludeArrays.push(void 0);
			continue;
		}
		const n = normalize(raw);
		if (!matchAll) if (n.include) includes.push(...n.include);
		else matchAll = true;
		excludeArrays.push(n.exclude);
	}
	return {
		includes: matchAll ? void 0 : includes.length > 0 ? includes : void 0,
		excludes: intersectArrays(excludeArrays, keyFn)
	};
}
/**
* Build the transform hook filter by intersecting a baseFilter (from user
* include/exclude options) with a presetFilter (union of all RolldownBabelPreset
* filters).
*
* - baseFilter constrains by id only (include/exclude from user options).
* - presetFilter constrains by id, moduleType, and code. Includes are unioned
*   across presets (OR), excludes are intersected (only patterns in ALL presets).
* - The result uses user includes when present, otherwise falls back to preset
*   includes. Excludes are combined from both (excluded by either → excluded).
* - If the user has explicit plugins, presetFilter is skipped (plugins can match
*   any file). Same if any preset is a plain babel preset without rolldown filters.
*/
function calculateTransformFilter(options) {
	const userInclude = extractStringOrRegExp(options.include);
	const userExclude = extractStringOrRegExp(options.exclude);
	const baseFilter = { id: {
		include: userInclude,
		exclude: userExclude
	} };
	if (options.plugins && options.plugins.length > 0) return baseFilter;
	const presets = options.presets;
	if (!presets || presets.length === 0) return baseFilter;
	for (const preset of presets) if (!isRolldownBabelPreset(preset) || !preset.rolldown.filter) return baseFilter;
	const filters = presets.map((p) => p.rolldown.filter);
	const idUnion = unionFilters(filters.map((f) => f.id), normalizeGeneralHookFilter, patternKey);
	const moduleTypeUnion = unionFilters(filters.map((f) => f.moduleType), (f) => {
		const types = normalizeModuleTypeFilter(f);
		return { include: types.length > 0 ? types : void 0 };
	}, (s) => s);
	const codeUnion = unionFilters(filters.map((f) => f.code), normalizeGeneralHookFilter, patternKey);
	const finalFilter = {};
	const finalFilterIdInclude = userInclude ?? idUnion.includes;
	const finalFilterIdExclude = concatArrays(userExclude, idUnion.excludes.length > 0 ? idUnion.excludes : void 0);
	if (finalFilterIdInclude || finalFilterIdExclude) finalFilter.id = {
		include: finalFilterIdInclude,
		exclude: finalFilterIdExclude
	};
	if (moduleTypeUnion.includes) finalFilter.moduleType = moduleTypeUnion.includes;
	if (codeUnion.includes) {
		const finalFilterCodeExclude = codeUnion.excludes.length > 0 ? codeUnion.excludes : void 0;
		if (finalFilterCodeExclude) finalFilter.code = {
			include: codeUnion.includes,
			exclude: finalFilterCodeExclude
		};
		else finalFilter.code = codeUnion.includes;
	}
	return finalFilter;
}
/**
* Calculate the filters to apply to the plugin
*/
function calculatePluginFilters(options) {
	return { transformFilter: calculateTransformFilter(options) };
}
//#endregion
//#region src/index.ts
async function babelPlugin(rawOptions) {
	if (rawOptions.runtimeVersion) try {
		import.meta.resolve("@babel/plugin-transform-runtime");
	} catch (err) {
		throw new Error(`Failed to load @babel/plugin-transform-runtime. Please install it to use the runtime option.`, { cause: err });
	}
	let configFilteredOptions;
	const envState = /* @__PURE__ */ new Map();
	const plugin = {
		name: "@rolldown/plugin-babel",
		enforce: "pre",
		config() {
			const include = collectOptimizeDepsInclude(rawOptions);
			if (include.length > 0) return { optimizeDeps: { include } };
		},
		configResolved(config) {
			configFilteredOptions = filterPresetsWithConfigResolved(rawOptions, config);
			const resolved = resolveOptions(configFilteredOptions);
			plugin.transform.filter = calculatePluginFilters(resolved).transformFilter;
		},
		applyToEnvironment(environment) {
			const envOptions = filterPresetsWithEnvironment(configFilteredOptions, environment);
			if (!envOptions.presets?.length && !envOptions.plugins?.length && !envOptions.overrides?.some((o) => o.presets?.length || o.plugins?.length)) return false;
			const resolved = resolveOptions(envOptions);
			envState.set(environment.name, createBabelOptionsConverter(resolved));
			return true;
		},
		outputOptions() {
			if (this.meta.viteVersion) return;
			const resolved = resolveOptions(rawOptions);
			envState.set(void 0, createBabelOptionsConverter(resolved));
			plugin.transform.filter = calculatePluginFilters(resolved).transformFilter;
		},
		transform: {
			filter: void 0,
			async handler(code, id, opts) {
				const convertToBabelOptions = envState.get(this.environment?.name);
				if (!convertToBabelOptions) return;
				const babelOptions = convertToBabelOptions({
					id,
					moduleType: opts?.moduleType ?? "js",
					code
				});
				const loadedOptions = await loadOptionsAsync({
					...babelOptions,
					babelrc: false,
					configFile: false,
					parserOpts: {
						sourceType: "module",
						allowAwaitOutsideFunction: true,
						...babelOptions.parserOpts
					},
					overrides: [
						{
							test: /\.jsx(?:$|\?)/,
							parserOpts: { plugins: ["jsx"] }
						},
						{
							test: /\.ts(?:$|\?)/,
							parserOpts: { plugins: ["typescript"] }
						},
						{
							test: /\.tsx(?:$|\?)/,
							parserOpts: { plugins: ["typescript", "jsx"] }
						},
						...babelOptions.overrides ?? []
					],
					filename: id
				});
				if (!loadedOptions || loadedOptions.plugins.length === 0) return;
				if (rawOptions.runtimeVersion) {
					loadedOptions.plugins ??= [];
					loadedOptions.plugins.push(["@babel/plugin-transform-runtime", { version: rawOptions.runtimeVersion }]);
				}
				let result;
				try {
					result = await babelCompat_exports.transformAsync(code, loadedOptions);
				} catch (err) {
					this.error({
						message: `[BabelError] ${err.message}`,
						loc: err.loc,
						pos: err.pos,
						cause: err,
						pluginCode: `${err.code}:${err.reasonCode}`
					});
				}
				if (result) return {
					code: result.code ?? void 0,
					map: result.map
				};
			}
		}
	};
	return plugin;
}
//#endregion
export { babelPlugin as default, defineRolldownBabelPreset };
