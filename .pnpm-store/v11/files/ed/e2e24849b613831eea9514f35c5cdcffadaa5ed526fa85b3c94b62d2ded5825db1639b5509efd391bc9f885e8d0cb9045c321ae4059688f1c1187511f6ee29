import "../../types.js";
import { getOxlintCommand, mapSeverity } from "./cli.js";
import path from "node:path";
//#region src/checkers/oxlint/options.ts
function resolveOptions(root, config) {
	const options = config === true ? { lintCommand: "oxlint" } : config;
	return {
		watchTarget: resolveWatchTarget(root, options.watchPath),
		logLevel: options.dev?.logLevel?.map((l) => mapSeverity(l)) ?? [0, 1],
		command: getOxlintCommand(options.lintCommand ?? "oxlint"),
		debounceMs: options.dev?.debounceMs
	};
}
function resolveWatchTarget(root, watchPath) {
	return Array.isArray(watchPath) ? watchPath.map((p) => path.resolve(root, p)) : typeof watchPath === "string" ? path.resolve(root, watchPath) : root;
}
//#endregion
export { resolveOptions };

//# sourceMappingURL=options.js.map