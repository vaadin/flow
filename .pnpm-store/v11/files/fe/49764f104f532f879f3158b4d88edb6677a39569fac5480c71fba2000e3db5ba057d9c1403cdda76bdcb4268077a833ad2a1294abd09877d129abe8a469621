import "../../types.js";
import { FileDiagnosticManager } from "../../FileDiagnosticManager.js";
import { composeCheckerSummary, consoleLog, diagnosticToConsoleLevel, diagnosticToRuntimeError, diagnosticToTerminalLog, toClientPayload } from "../../logger.js";
import { resolveOptions } from "./options.js";
import { setupDevServer } from "./server.js";
import { parentPort } from "node:worker_threads";
//#region src/checkers/oxlint/diagnostics.ts
const createDiagnostic = (pluginConfig) => {
	const manager = new FileDiagnosticManager();
	const oxlintConfig = pluginConfig.oxlint;
	const displayTargets = /* @__PURE__ */ new Set();
	return {
		config: async ({ enableOverlay, enableTerminal }) => {
			displayTargets.clear();
			if (enableOverlay) displayTargets.add("overlay");
			if (enableTerminal) displayTargets.add("terminal");
		},
		async configureServer({ root }) {
			if (!oxlintConfig) return;
			await setupDevServer(root, resolveOptions(root, oxlintConfig), manager, displayTargets);
		}
	};
};
function dispatchDiagnostics(diagnostics, targets) {
	if (targets.size === 0) return;
	if (targets.has("terminal")) dispatchTerminalDiagnostics(diagnostics);
	if (targets.has("overlay")) dispatchOverlayDiagnostics(diagnostics);
}
function dispatchTerminalDiagnostics(diagnostics) {
	for (const d of diagnostics) consoleLog(diagnosticToTerminalLog(d, "oxlint"), diagnosticToConsoleLevel(d));
	const errorCount = diagnostics.filter((d) => d.level === 1).length;
	const warningCount = diagnostics.filter((d) => d.level === 0).length;
	consoleLog(composeCheckerSummary("oxlint", errorCount, warningCount), errorCount ? "error" : warningCount ? "warn" : "info");
}
function dispatchOverlayDiagnostics(diagnostics) {
	parentPort?.postMessage({
		type: "overlayError",
		payload: toClientPayload("oxlint", diagnostics.map((d) => diagnosticToRuntimeError(d)))
	});
}
//#endregion
export { createDiagnostic, dispatchDiagnostics };

//# sourceMappingURL=diagnostics.js.map