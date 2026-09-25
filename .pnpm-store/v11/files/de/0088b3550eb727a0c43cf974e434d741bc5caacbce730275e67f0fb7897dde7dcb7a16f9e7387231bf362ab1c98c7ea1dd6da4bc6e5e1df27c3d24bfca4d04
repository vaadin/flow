import "../../types.js";
import { createFrame, offsetRangeToBabelLocation } from "../../codeFrame.js";
import { consoleLog } from "../../logger.js";
import { normalizePath, readSources } from "../../sources.js";
import parseArgsStringToArgv from "../stylelint/argv.js";
import { stripVTControlCharacters } from "node:util";
import colors from "picocolors";
import { execFile } from "node:child_process";
//#region src/checkers/oxlint/cli.ts
const severityMap = {
	error: 1,
	warning: 0,
	info: 2
};
function mapSeverity(s) {
	return severityMap[s] ?? 1;
}
function getOxlintCommand(command) {
	const parsed = parseArgsStringToArgv(command);
	const index = parsed.findIndex((p) => p === "--format" || p === "-f");
	if (index === -1) parsed.push("--format", "json");
	else {
		consoleLog(colors.yellow("vite-plugin-checker will force append \"--format json\" to the flags in dev mode, please don't use \"--format\" or \"-f\" flag in \"config.oxlint.lintCommand\"."), "warn");
		parsed.splice(index, 2, "--format", "json");
	}
	return parsed;
}
function runOxlint(argv, cwd) {
	return new Promise((resolve, _reject) => {
		try {
			execFile(argv[0], argv.slice(1), {
				cwd,
				maxBuffer: Number.POSITIVE_INFINITY,
				shell: process.platform === "win32"
			}, (_error, stdout, _stderr) => {
				parseOxlintOutput(stdout, cwd).then(resolve).catch(() => resolve([]));
			}).on("error", (error) => {
				logSpawnError(error);
				resolve([]);
			});
		} catch (error) {
			logSpawnError(error);
			resolve([]);
		}
	});
}
function logSpawnError(error) {
	const message = error instanceof Error ? error.message : String(error);
	consoleLog(colors.yellow(`vite-plugin-checker failed to spawn oxlint: ${message}`), "warn");
}
async function parseOxlintOutput(output, cwd) {
	const parsed = safeParseOxlint(output);
	if (!parsed) return [];
	const entries = getEntries(parsed, cwd);
	if (entries.length === 0) return [];
	return buildDiagnostics(entries, await readSources(getUniqueFiles(entries)));
}
function safeParseOxlint(output) {
	try {
		return JSON.parse(output);
	} catch {
		return null;
	}
}
function getEntries(parsed, cwd) {
	return parsed.diagnostics.flatMap(({ filename, labels, code, message, severity }) => {
		const file = normalizePath(filename, cwd);
		const [label] = labels;
		if (!label) return [];
		return [{
			file,
			span: label.span,
			code,
			message,
			severity
		}];
	});
}
function getUniqueFiles(entries) {
	return [...new Set(entries.map((e) => e.file))];
}
function buildDiagnostics(entries, sources) {
	return entries.flatMap((entry) => {
		const source = sources.get(entry.file);
		if (!source) return [];
		const loc = offsetRangeToBabelLocation(source, entry.span.offset, entry.span.length);
		const codeFrame = createFrame(source, loc);
		return [{
			message: `${entry.code}: ${entry.message}`,
			conclusion: "",
			level: mapSeverity(entry.severity),
			checker: "oxlint",
			id: entry.file,
			codeFrame,
			stripedCodeFrame: codeFrame && stripVTControlCharacters(codeFrame),
			loc
		}];
	});
}
//#endregion
export { getOxlintCommand, mapSeverity, runOxlint };

//# sourceMappingURL=cli.js.map