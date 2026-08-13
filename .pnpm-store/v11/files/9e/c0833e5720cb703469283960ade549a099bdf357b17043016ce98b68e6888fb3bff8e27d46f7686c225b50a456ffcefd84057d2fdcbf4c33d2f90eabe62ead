import { filterLogLevel } from "../../logger.js";
import { applyBatchedDiagnostics } from "../_shared/applyBatchedDiagnostics.js";
import { createLintScheduler } from "../_shared/lintScheduler.js";
import { runOxlint } from "./cli.js";
import { dispatchDiagnostics } from "./diagnostics.js";
import path from "node:path";
import chokidar from "chokidar";
//#region src/checkers/oxlint/server.ts
async function setupDevServer(root, options, manager, displayTargets) {
	const initial = await runOxlint(options.command, root);
	manager.initWith(initial);
	dispatchDiagnostics(filterLogLevel(manager.getDiagnostics(), options.logLevel), displayTargets);
	const dispatch = () => dispatchDiagnostics(filterLogLevel(manager.getDiagnostics(), options.logLevel), displayTargets);
	const scheduler = createLintScheduler({
		debounceMs: options.debounceMs ?? 300,
		onBatch: async (files) => {
			if (files.some((f) => path.basename(f) === ".oxlintrc.json")) {
				const diagnostics = await runOxlint([...options.command, root], root);
				manager.initWith(diagnostics);
			} else applyBatchedDiagnostics(manager, files, await runOxlint([...options.command, ...files], root), root);
			dispatch();
		}
	});
	const watcher = chokidar.watch(options.watchTarget, {
		cwd: root,
		ignored: (p) => p.includes("node_modules")
	});
	watcher.on("change", (filePath) => {
		scheduler.schedule(path.resolve(root, filePath));
	});
	watcher.on("unlink", (filePath) => {
		const absPath = path.resolve(root, filePath);
		manager.updateByFileId(absPath, []);
		dispatch();
	});
}
//#endregion
export { setupDevServer };

//# sourceMappingURL=server.js.map