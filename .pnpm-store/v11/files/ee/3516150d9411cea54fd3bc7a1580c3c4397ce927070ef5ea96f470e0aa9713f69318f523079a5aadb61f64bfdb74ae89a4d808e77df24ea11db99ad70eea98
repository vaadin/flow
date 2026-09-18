import "../../types.js";
import { createFrame } from "../../codeFrame.js";
import { normalizePath, readSources } from "../../sources.js";
import parseArgsStringToArgv from "../stylelint/argv.js";
import { stripVTControlCharacters } from "node:util";
import { execFile } from "node:child_process";
//#region src/checkers/biome/cli.ts
const severityMap = {
	error: 1,
	warning: 0,
	info: 2,
	information: 2
};
function getBiomeCommand(command, flags, files) {
	if (flags.includes("--flags")) throw Error(`vite-plugin-checker will force append "--reporter json" to the flags in dev mode, please don't use "--flags" in "config.biome.flags".
If you need to customize "--flags" in build mode, please use "config.biome.build.flags" instead.`);
	return [
		"biome",
		command,
		...flags ? parseArgsStringToArgv(flags) : [],
		"--reporter",
		"json",
		...files
	];
}
function runBiome(argv, cwd) {
	return new Promise((resolve, _reject) => {
		execFile(argv[0], argv.slice(1), {
			cwd,
			maxBuffer: Number.POSITIVE_INFINITY,
			shell: process.platform === "win32"
		}, (_error, stdout, _stderr) => {
			parseBiomeOutput(stdout, cwd).then(resolve).catch(() => resolve([]));
		});
	});
}
function isModernDiagnostic(d) {
	return d.location !== void 0 && typeof d.location.path === "string";
}
function isLegacyDiagnostic(d) {
	return d.location !== void 0 && typeof d.location.path === "object" && d.location.path !== null && "file" in d.location.path;
}
function getEntries(parsed, cwd) {
	return parsed.diagnostics.flatMap((d) => {
		if (!d.location) return [];
		if (isModernDiagnostic(d)) return [{
			file: normalizePath(d.location.path, cwd),
			message: d.message,
			category: d.category ?? "",
			severity: d.severity,
			start: d.location.start,
			end: d.location.end
		}];
		if (isLegacyDiagnostic(d)) return [{
			file: normalizePath(d.location.path?.file ?? "", cwd),
			message: d.description,
			category: d.category ?? "",
			severity: d.severity,
			start: getLineAndColumn(d.location.sourceCode, d.location.span?.[0]),
			end: getLineAndColumn(d.location.sourceCode, d.location.span?.[1]),
			sourceCode: d.location.sourceCode
		}];
		return [];
	});
}
function getUniqueFiles(entries) {
	return Array.from(new Set(entries.map((e) => e.file)));
}
function buildDiagnostics(entries, sources) {
	return entries.flatMap((entry) => {
		const source = entry.sourceCode ?? sources.get(entry.file);
		if (!source) return [];
		const loc = {
			file: entry.file,
			start: entry.start,
			end: entry.end
		};
		const codeFrame = createFrame(source, loc);
		return [{
			message: `[${entry.category}] ${entry.message}`,
			level: severityMap[entry.severity] ?? 1,
			checker: "Biome",
			id: entry.file,
			codeFrame,
			stripedCodeFrame: codeFrame && stripVTControlCharacters(codeFrame),
			loc
		}];
	});
}
function sanitizeBiomeOutput(output) {
	return output.replace(/\\(?!["\\/bfnrtu])/g, "\\\\");
}
/**
* Convert a byte-offset into `text` to a 1-based line/column pair.
* Used only for the legacy Biome schema (< 2.4) which reports positions
* as byte offsets into the embedded `sourceCode`.
*/
function getLineAndColumn(text, offset) {
	if (!text || !offset) return {
		line: 0,
		column: 0
	};
	let line = 1;
	let column = 1;
	for (let i = 0; i < offset; i++) if (text[i] === "\n") {
		line++;
		column = 1;
	} else column++;
	return {
		line,
		column
	};
}
async function parseBiomeOutput(output, cwd) {
	let parsed;
	try {
		parsed = JSON.parse(sanitizeBiomeOutput(output));
	} catch {
		return [];
	}
	const entries = getEntries(parsed, cwd);
	return buildDiagnostics(entries, await readSources(getUniqueFiles(entries.filter((e) => !e.sourceCode))));
}
//#endregion
export { getBiomeCommand, runBiome, severityMap };

//# sourceMappingURL=cli.js.map