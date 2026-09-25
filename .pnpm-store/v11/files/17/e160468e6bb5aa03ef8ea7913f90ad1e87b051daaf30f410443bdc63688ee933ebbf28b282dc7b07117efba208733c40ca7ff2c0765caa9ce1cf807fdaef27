import { isMainThread as isMainThread$1 } from "./utils.js";
import "./types.js";
import { createFrame, lineColLocToBabelLoc, tsLikeLocToBabelLoc } from "./codeFrame.js";
import { WS_CHECKER_ERROR_EVENT } from "./client/index.js";
import { createRequire } from "node:module";
import { parentPort } from "node:worker_threads";
import os from "node:os";
import { stripVTControlCharacters } from "node:util";
import colors from "picocolors";
//#region src/logger.ts
const _require = createRequire(import.meta.url);
const defaultLogLevel = [
	0,
	1,
	2,
	3
];
function filterLogLevel(diagnostics, level = defaultLogLevel) {
	if (Array.isArray(diagnostics)) return diagnostics.filter((d) => {
		if (typeof d.level !== "number") return false;
		return level.includes(d.level);
	});
	if (!diagnostics.level) return null;
	return level.includes(diagnostics.level) ? diagnostics : null;
}
function diagnosticToTerminalLog(d, name) {
	const nameInLabel = name ? `(${name})` : "";
	const boldBlack = (str) => colors.bold(colors.black(str));
	const levelLabel = {
		[1]: boldBlack(colors.bgRedBright(` ERROR${nameInLabel} `)),
		[0]: boldBlack(colors.bgYellowBright(` WARNING${nameInLabel} `)),
		[2]: boldBlack(colors.bgBlueBright(` SUGGESTION${nameInLabel} `)),
		[3]: boldBlack(colors.bgCyanBright(` MESSAGE${nameInLabel} `))
	}[d.level ?? 1];
	const fileLabel = `${boldBlack(colors.bgCyanBright(" FILE "))} `;
	const position = d.loc ? `${colors.yellow(d.loc.start.line)}:${colors.yellow(d.loc.start.column || "")}` : "";
	return [
		`${levelLabel} ${d.message}`,
		`${fileLabel + d.id}:${position}${os.EOL}`,
		d.codeFrame + os.EOL,
		d.conclusion
	].filter(Boolean).join(os.EOL);
}
function diagnosticToConsoleLevel(d) {
	if (!d) return "error";
	if (d.level === 3) return "info";
	if (d.level === 2) return "info";
	if (d.level === 0) return "warn";
	return "error";
}
function diagnosticToRuntimeError(diagnostics) {
	const results = (Array.isArray(diagnostics) ? diagnostics : [diagnostics]).map((d) => {
		let loc;
		if (d.loc) loc = {
			file: d.id ?? "",
			line: d.loc.start.line,
			column: typeof d.loc.start.column === "number" ? d.loc.start.column : 0
		};
		return {
			message: d.message ?? "",
			stack: typeof d.stack === "string" ? d.stack : Array.isArray(d.stack) ? d.stack.join(os.EOL) : "",
			id: d.id,
			frame: d.stripedCodeFrame,
			checkerId: d.checker,
			level: d.level,
			loc
		};
	});
	return Array.isArray(diagnostics) ? results : results[0];
}
function toClientPayload(id, diagnostics) {
	return {
		event: WS_CHECKER_ERROR_EVENT,
		data: {
			checkerId: id,
			diagnostics
		}
	};
}
function wrapCheckerSummary(checkerName, rawSummary) {
	return `[${checkerName}] ${rawSummary}`;
}
function composeCheckerSummary(checkerName, errorCount, warningCount) {
	const message = `Found ${errorCount} error${errorCount > 1 ? "s" : ""} and ${warningCount} warning${warningCount > 1 ? "s" : ""}`;
	return colors[errorCount > 0 ? "red" : warningCount > 0 ? "yellow" : "green"](wrapCheckerSummary(checkerName, message));
}
function normalizeTsDiagnostic(d) {
	const fileName = d.file?.fileName;
	const { flattenDiagnosticMessageText } = _require("typescript");
	const message = flattenDiagnosticMessageText(d.messageText, os.EOL);
	let loc;
	const pos = d.start === void 0 ? null : d.file?.getLineAndCharacterOfPosition?.(d.start);
	if (pos && d.file && typeof d.start === "number" && typeof d.length === "number") loc = tsLikeLocToBabelLoc({
		start: pos,
		end: d.file.getLineAndCharacterOfPosition(d.start + d.length)
	});
	let codeFrame;
	if (loc) codeFrame = createFrame(d.file.text, loc);
	return {
		message,
		conclusion: "",
		codeFrame,
		stripedCodeFrame: codeFrame && stripVTControlCharacters(codeFrame),
		id: fileName,
		checker: "TypeScript",
		loc,
		level: d.category
	};
}
function normalizeVueTscDiagnostic(d) {
	const diagnostic = normalizeTsDiagnostic(d);
	diagnostic.checker = "vue-tsc";
	return diagnostic;
}
const isNormalizedDiagnostic = (d) => {
	return Boolean(d);
};
function normalizeEslintDiagnostic(diagnostic) {
	return diagnostic.messages.map((d) => {
		let level = 1;
		switch (d.severity) {
			case 0:
				level = 1;
				return null;
			case 1:
				level = 0;
				break;
			case 2:
				level = 1;
				break;
		}
		const loc = lineColLocToBabelLoc(d);
		const codeFrame = createFrame(diagnostic.source ?? "", loc);
		return {
			message: `${d.message} (${d.ruleId})`,
			conclusion: "",
			codeFrame,
			stripedCodeFrame: codeFrame && stripVTControlCharacters(codeFrame),
			id: diagnostic.filePath,
			checker: "ESLint",
			loc,
			level
		};
	}).filter(isNormalizedDiagnostic);
}
function normalizeStylelintDiagnostic(diagnostic) {
	return diagnostic.warnings.map((d) => {
		let level = 1;
		switch (d.severity) {
			case "warning":
				level = 0;
				break;
			case "error":
				level = 1;
				break;
			default:
				level = 1;
				return null;
		}
		const loc = lineColLocToBabelLoc(d);
		const codeFrame = createFrame(diagnostic._postcssResult?.css ?? "", loc);
		return {
			message: `${d.text} (${d.rule})`,
			conclusion: "",
			codeFrame,
			stripedCodeFrame: codeFrame && stripVTControlCharacters(codeFrame),
			id: diagnostic.source,
			checker: "Stylelint",
			loc,
			level
		};
	}).filter(isNormalizedDiagnostic);
}
function ensureCall(callback) {
	setTimeout(() => {
		callback();
	});
}
function consoleLog(value, level) {
	if (isMainThread$1) console[level](value);
	else parentPort?.postMessage({
		type: "console",
		level,
		payload: value
	});
}
//#endregion
export { composeCheckerSummary, consoleLog, diagnosticToConsoleLevel, diagnosticToRuntimeError, diagnosticToTerminalLog, ensureCall, filterLogLevel, normalizeEslintDiagnostic, normalizeStylelintDiagnostic, normalizeTsDiagnostic, normalizeVueTscDiagnostic, toClientPayload, wrapCheckerSummary };

//# sourceMappingURL=logger.js.map