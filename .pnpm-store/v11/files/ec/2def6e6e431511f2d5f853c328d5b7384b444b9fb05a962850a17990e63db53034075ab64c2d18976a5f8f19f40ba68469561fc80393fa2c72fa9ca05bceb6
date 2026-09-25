import { Checker } from "../../Checker.js";
import parseArgsStringToArgv from "../stylelint/argv.js";
import { createDiagnostic } from "./diagnostics.js";
import { fileURLToPath } from "node:url";
//#region src/checkers/oxlint/main.ts
const __filename = fileURLToPath(import.meta.url);
var OxlintChecker = class extends Checker {
	constructor() {
		super({
			name: "oxlint",
			absFilePath: __filename,
			build: { buildBin: ({ oxlint }) => {
				const command = parseArgsStringToArgv(typeof oxlint === "boolean" ? "oxlint" : oxlint?.lintCommand ?? "oxlint");
				return [command[0], command.slice(1)];
			} },
			createDiagnostic
		});
	}
};
const oxlint = new OxlintChecker();
oxlint.prepare();
oxlint.initWorkerThread();
const createServeAndBuild = oxlint.initMainThread();
//#endregion
export { OxlintChecker, createServeAndBuild };

//# sourceMappingURL=main.js.map