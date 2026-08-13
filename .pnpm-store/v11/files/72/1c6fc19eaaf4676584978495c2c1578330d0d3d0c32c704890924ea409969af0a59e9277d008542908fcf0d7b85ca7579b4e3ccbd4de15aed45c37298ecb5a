import { isInVitestEntryThread, isMainThread } from "./utils.js";
import { createScript } from "./worker.js";
import invariant from "tiny-invariant";
//#region src/Checker.ts
if (!(isMainThread || isInVitestEntryThread)) process.stdout.isTTY = true;
var Checker = class Checker {
	static logger = [];
	static log(...args) {
		for (const fn of Checker.logger) fn(...args);
	}
	name;
	absFilePath;
	createDiagnostic;
	build;
	script;
	constructor({ name, absFilePath, createDiagnostic, build }) {
		this.name = name;
		this.absFilePath = absFilePath;
		this.build = build;
		this.createDiagnostic = createDiagnostic;
		this.build = build;
	}
	prepare() {
		const script = createScript({
			absFilename: this.absFilePath,
			buildBin: this.build.buildBin,
			serverChecker: { createDiagnostic: this.createDiagnostic }
		});
		this.script = script;
		return script;
	}
	initMainThread() {
		invariant(this.script, `script should be created in 'prepare', but got ${this.script}`);
		if (isMainThread || isInVitestEntryThread) return this.script.mainScript();
	}
	initWorkerThread() {
		invariant(this.script, `script should be created in 'prepare', but got ${this.script}`);
		if (!(isMainThread || isInVitestEntryThread)) this.script.workerScript();
	}
};
//#endregion
export { Checker };

//# sourceMappingURL=Checker.js.map