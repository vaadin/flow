import { ignoreTransientFsError } from "../../utils.js";
import "../../types.js";
import { Checker } from "../../Checker.js";
import { FileDiagnosticManager } from "../../FileDiagnosticManager.js";
import { createIgnore } from "../../glob.js";
import { composeCheckerSummary, consoleLog, diagnosticToConsoleLevel, diagnosticToRuntimeError, diagnosticToTerminalLog, filterLogLevel, normalizeEslintDiagnostic, toClientPayload } from "../../logger.js";
import { applyBatchedDiagnostics } from "../_shared/applyBatchedDiagnostics.js";
import { createLintScheduler } from "../_shared/lintScheduler.js";
import { translateOptions } from "./cli.js";
import { options } from "./options.js";
import Module from "node:module";
import invariant from "tiny-invariant";
import { parentPort } from "node:worker_threads";
import path from "node:path";
import { fileURLToPath } from "node:url";
import chokidar from "chokidar";
//#region src/checkers/eslint/main.ts
const __filename = fileURLToPath(import.meta.url);
const _require = Module.createRequire(import.meta.url);
const manager = new FileDiagnosticManager();
let createServeAndBuild;
/**
* Detect the installed ESLint major version.
*/
function getEslintMajorVersion() {
	try {
		const eslintPkg = _require("eslint/package.json");
		return Number.parseInt(eslintPkg.version.split(".")[0], 10);
	} catch {
		return 10;
	}
}
/**
* Resolve the correct ESLint class based on version and config mode.
*
* - ESLint v10+: Only flat config is supported, use `ESLint` directly.
* - ESLint v9 with flat config (default): Use `ESLint` directly (it's the flat config class in v9).
* - ESLint v9 with legacy eslintrc: Use `LegacyESLint` from `eslint/use-at-your-own-risk`.
*/
async function resolveEslintClass(useFlatConfig, majorVersion) {
	const { ESLint } = await import("eslint");
	if (majorVersion >= 10) return ESLint;
	if (useFlatConfig) return ESLint;
	try {
		const { LegacyESLint } = _require("eslint/use-at-your-own-risk");
		if (LegacyESLint) return LegacyESLint;
	} catch {}
	throw new Error("Legacy eslintrc mode (useFlatConfig: false) requires ESLint v9. ESLint v10+ only supports flat config. Please upgrade your config to flat config or set useFlatConfig to true.");
}
const createDiagnostic = (pluginConfig) => {
	let overlay = true;
	let terminal = true;
	return {
		config: async ({ enableOverlay, enableTerminal }) => {
			overlay = enableOverlay;
			terminal = enableTerminal;
		},
		async configureServer({ root }) {
			if (!pluginConfig.eslint) return;
			const majorVersion = getEslintMajorVersion();
			const useFlatConfig = pluginConfig.eslint.useFlatConfig ?? true;
			if (!useFlatConfig && majorVersion >= 10) console.warn("[vite-plugin-checker] useFlatConfig: false is not supported with ESLint v10+. Falling back to flat config mode.");
			const effectiveUseFlatConfig = majorVersion >= 10 ? true : useFlatConfig;
			const options$1 = options.parse(pluginConfig.eslint.lintCommand);
			invariant(!options$1.fix, "Using `--fix` in `config.eslint.lintCommand` is not allowed in vite-plugin-checker, you could using `--fix` with editor.");
			const translatedOptions = translateOptions(options$1, effectiveUseFlatConfig);
			const logLevel = (() => {
				if (typeof pluginConfig.eslint !== "object") return void 0;
				const userLogLevel = pluginConfig.eslint.dev?.logLevel;
				if (!userLogLevel) return void 0;
				const map = {
					error: 1,
					warning: 0
				};
				return userLogLevel.map((l) => map[l]);
			})();
			const eslintOptions = {
				cwd: root,
				...translatedOptions,
				...pluginConfig.eslint.dev?.overrideConfig
			};
			const eslint = new (await (resolveEslintClass(effectiveUseFlatConfig, majorVersion)))(eslintOptions);
			const dispatchDiagnostics = () => {
				const diagnostics = filterLogLevel(manager.getDiagnostics(), logLevel);
				if (terminal) {
					for (const d of diagnostics) consoleLog(diagnosticToTerminalLog(d, "ESLint"), diagnosticToConsoleLevel(d));
					const errorCount = diagnostics.filter((d) => d.level === 1).length;
					const warningCount = diagnostics.filter((d) => d.level === 0).length;
					consoleLog(composeCheckerSummary("ESLint", errorCount, warningCount), errorCount ? "error" : warningCount ? "warn" : "info");
				}
				if (overlay) parentPort?.postMessage({
					type: "overlayError",
					payload: toClientPayload("eslint", diagnostics.map((d) => diagnosticToRuntimeError(d)))
				});
			};
			const scheduler = createLintScheduler({
				debounceMs: pluginConfig.eslint.dev?.debounceMs ?? 300,
				onBatch: async (files) => {
					applyBatchedDiagnostics(manager, files, (await eslint.lintFiles(files)).flatMap((d) => normalizeEslintDiagnostic(d)), root);
					dispatchDiagnostics();
				}
			});
			const shouldLintPath = async (absPath) => {
				if (!effectiveUseFlatConfig) {
					const extension = path.extname(absPath);
					const { extensions } = eslintOptions;
					if (Array.isArray(extensions) && !extensions.includes(extension)) return false;
				}
				return !await eslint.isPathIgnored(absPath);
			};
			const files = options$1._.slice(1);
			const diagnostics = await eslint.lintFiles(files);
			manager.initWith(diagnostics.flatMap((p) => normalizeEslintDiagnostic(p)));
			dispatchDiagnostics();
			let watchTarget = root;
			if (pluginConfig.eslint.watchPath) if (Array.isArray(pluginConfig.eslint.watchPath)) watchTarget = pluginConfig.eslint.watchPath.map((p) => path.resolve(root, p));
			else watchTarget = path.resolve(root, pluginConfig.eslint.watchPath);
			const watcher = chokidar.watch(watchTarget, {
				cwd: root,
				ignored: createIgnore(root, files)
			});
			watcher.on("change", (filePath) => {
				const absPath = path.resolve(root, filePath);
				shouldLintPath(absPath).then((shouldLint) => {
					if (shouldLint) scheduler.schedule(absPath);
				}).catch(ignoreTransientFsError);
			});
			watcher.on("unlink", (filePath) => {
				const absPath = path.resolve(root, filePath);
				manager.updateByFileId(absPath, []);
				dispatchDiagnostics();
			});
		}
	};
};
var EslintChecker = class extends Checker {
	constructor() {
		super({
			name: "eslint",
			absFilePath: __filename,
			build: { buildBin: (pluginConfig) => {
				if (pluginConfig.eslint) {
					const { lintCommand } = pluginConfig.eslint;
					return ["eslint", lintCommand.split(" ").slice(1)];
				}
				return ["eslint", [""]];
			} },
			createDiagnostic
		});
	}
	init() {
		createServeAndBuild = super.initMainThread();
		super.initWorkerThread();
	}
};
const eslintChecker = new EslintChecker();
eslintChecker.prepare();
eslintChecker.init();
//#endregion
export { EslintChecker, createServeAndBuild };

//# sourceMappingURL=main.js.map