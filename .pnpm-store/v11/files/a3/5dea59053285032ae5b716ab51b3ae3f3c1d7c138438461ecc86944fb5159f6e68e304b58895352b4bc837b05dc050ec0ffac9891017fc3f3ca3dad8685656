import "../../types.js";
import { Checker } from "../../Checker.js";
import { createFrame } from "../../codeFrame.js";
import { consoleLog, diagnosticToRuntimeError, diagnosticToTerminalLog, ensureCall, normalizeTsDiagnostic, toClientPayload, wrapCheckerSummary } from "../../logger.js";
import { forceNoEmitOnSolutionBuilderHost } from "../tscUtils.js";
import invariant from "tiny-invariant";
import { parentPort } from "node:worker_threads";
import os from "node:os";
import path from "node:path";
import colors from "picocolors";
import { fileURLToPath, pathToFileURL } from "node:url";
//#region src/checkers/typescript/main.ts
const __filename = fileURLToPath(import.meta.url);
let createServeAndBuild;
const createDiagnostic = (pluginConfig) => {
	let overlay = true;
	let terminal = true;
	let currDiagnostics = [];
	return {
		config: async ({ enableOverlay, enableTerminal }) => {
			overlay = enableOverlay;
			terminal = enableTerminal;
		},
		async configureServer({ root }) {
			invariant(pluginConfig.typescript, "config.typescript should be `false`");
			const finalConfig = pluginConfig.typescript === true ? {
				root,
				tsconfigPath: "tsconfig.json",
				typescriptPath: "typescript"
			} : {
				root: pluginConfig.typescript.root ?? root,
				tsconfigPath: pluginConfig.typescript.tsconfigPath ?? "tsconfig.json",
				typescriptPath: pluginConfig.typescript.typescriptPath ?? "typescript"
			};
			let configFile;
			const ts = await (path.isAbsolute(finalConfig.typescriptPath) ? import(pathToFileURL(finalConfig.typescriptPath).href) : import(finalConfig.typescriptPath)).then((r) => r.default || r);
			if (!ts.sys) {
				const { spawn } = await import("node:child_process");
				const { existsSync, readFileSync } = await import("node:fs");
				const { createRequire } = await import("node:module");
				const { stripVTControlCharacters: strip } = await import("node:util");
				const tsconfigPath = path.resolve(finalConfig.root, finalConfig.tsconfigPath);
				if (!existsSync(tsconfigPath)) throw new Error(`Failed to find tsconfig.json: ${tsconfigPath}`);
				const isBuildMode = typeof pluginConfig.typescript === "object" && pluginConfig.typescript.buildMode;
				const args = [
					...isBuildMode ? ["-b"] : ["--noEmit"],
					"--watch",
					"--pretty",
					"false"
				];
				if (isBuildMode) args.push(tsconfigPath);
				else args.push("-p", tsconfigPath);
				let tscBin = "tsc";
				let runWithNode = false;
				try {
					const tsEntry = createRequire(path.join(finalConfig.root, "noop.js")).resolve(finalConfig.typescriptPath);
					let dir = path.dirname(tsEntry);
					while (dir !== path.dirname(dir)) {
						if (existsSync(path.join(dir, "package.json"))) break;
						dir = path.dirname(dir);
					}
					tscBin = path.join(dir, "bin", "tsc");
					runWithNode = existsSync(tscBin);
				} catch {}
				const tscEnv = {
					...process.env,
					NODE_NO_WARNINGS: "1"
				};
				const tscProcess = runWithNode ? spawn(process.execPath, [tscBin, ...args], {
					cwd: finalConfig.root,
					env: tscEnv
				}) : spawn(tscBin, args, {
					cwd: finalConfig.root,
					env: tscEnv,
					shell: true
				});
				let logChunk = "";
				let stdoutBuffer = "";
				let pendingDiag = null;
				const flushPendingDiag = () => {
					if (!pendingDiag) return;
					currDiagnostics.push(diagnosticToRuntimeError(pendingDiag));
					logChunk += os.EOL + diagnosticToTerminalLog(pendingDiag, "TypeScript");
					pendingDiag = null;
				};
				const handleLine = (rawLine) => {
					const line = rawLine.replace(/\r$/, "").replace(/^\d{1,2}:\d{2}:\d{2}\s+[AP]M\s+-\s+/, "");
					const diagMatch = line.match(/^(.+?)\((\d+),(\d+)\): (error|warning) TS(\d+): (.+)$/);
					if (diagMatch) {
						flushPendingDiag();
						const [, file, lineStr, colStr, severity, tsCode, message] = diagMatch;
						const lineNum = +lineStr;
						const colNum = +colStr;
						const absFile = path.resolve(finalConfig.root, file);
						let codeFrame;
						let stripedCodeFrame;
						if (existsSync(absFile)) try {
							codeFrame = createFrame(readFileSync(absFile, "utf-8"), { start: {
								line: lineNum,
								column: colNum
							} });
							stripedCodeFrame = strip(codeFrame);
						} catch {}
						pendingDiag = {
							message: `TS${tsCode}: ${message}`,
							conclusion: "",
							id: absFile,
							checker: "TypeScript",
							codeFrame,
							stripedCodeFrame,
							loc: { start: {
								line: lineNum,
								column: colNum
							} },
							level: severity === "warning" ? 0 : 1
						};
						return;
					}
					const summaryMatch = line.match(/Found (\d+) errors?\./);
					if (summaryMatch) {
						flushPendingDiag();
						const errorCount = +summaryMatch[1];
						if (overlay) parentPort?.postMessage({
							type: "overlayError",
							payload: toClientPayload("typescript", currDiagnostics)
						});
						const capturedLogChunk = logChunk;
						ensureCall(() => {
							if (terminal) consoleLog(colors[errorCount > 0 ? "red" : "green"]((errorCount > 0 ? capturedLogChunk : "") + os.EOL + wrapCheckerSummary("TypeScript", errorCount > 0 ? `Found ${errorCount} error(s)` : "No errors")), errorCount > 0 ? "error" : "info");
						});
						logChunk = "";
						currDiagnostics = [];
						return;
					}
					if (pendingDiag && line.trim() !== "") pendingDiag.message += os.EOL + line;
				};
				tscProcess.stdout.on("data", (chunk) => {
					stdoutBuffer += chunk.toString();
					const lines = stdoutBuffer.split("\n");
					stdoutBuffer = lines.pop() ?? "";
					for (const rawLine of lines) handleLine(rawLine);
				});
				tscProcess.stderr.on("data", (chunk) => {
					consoleLog(colors.red(chunk.toString()), "error");
				});
				tscProcess.on("error", (err) => {
					consoleLog(colors.red(`TypeScript checker failed: ${err.message}`), "error");
				});
				const killTsc = () => {
					tscProcess.kill();
				};
				parentPort?.on("close", killTsc);
				process.on("exit", killTsc);
				return;
			}
			configFile = ts.findConfigFile(finalConfig.root, ts.sys.fileExists, finalConfig.tsconfigPath);
			if (configFile === void 0) throw Error(`Failed to find a valid tsconfig.json: ${finalConfig.tsconfigPath} at ${finalConfig.root} is not a valid tsconfig`);
			let logChunk = "";
			const reportDiagnostic = (diagnostic) => {
				const normalizedDiagnostic = normalizeTsDiagnostic(diagnostic);
				if (normalizedDiagnostic === null) return;
				currDiagnostics.push(diagnosticToRuntimeError(normalizedDiagnostic));
				logChunk += os.EOL + diagnosticToTerminalLog(normalizedDiagnostic, "TypeScript");
			};
			const reportWatchStatusChanged = (diagnostic, _newLine, _options, errorCount) => {
				if (diagnostic.code === 6031) return;
				switch (diagnostic.code) {
					case 6031:
					case 6032:
						logChunk = "";
						currDiagnostics = [];
						return;
					case 6193:
					case 6194: if (overlay) parentPort?.postMessage({
						type: "overlayError",
						payload: toClientPayload("typescript", currDiagnostics)
					});
				}
				ensureCall(() => {
					if (errorCount === 0) logChunk = "";
					if (terminal) consoleLog(colors[errorCount && errorCount > 0 ? "red" : "green"](logChunk + os.EOL + wrapCheckerSummary("TypeScript", diagnostic.messageText.toString())), errorCount ? "error" : "info");
				});
			};
			const createProgram = ts.createEmitAndSemanticDiagnosticsBuilderProgram;
			if (typeof pluginConfig.typescript === "object" && pluginConfig.typescript.buildMode) {
				const host = forceNoEmitOnSolutionBuilderHost(ts, ts.createSolutionBuilderWithWatchHost(ts.sys, createProgram, reportDiagnostic, void 0, reportWatchStatusChanged));
				ts.createSolutionBuilderWithWatch(host, [configFile], {}).build();
			} else {
				const host = ts.createWatchCompilerHost(configFile, { noEmit: true }, ts.sys, createProgram, reportDiagnostic, reportWatchStatusChanged);
				ts.createWatchProgram(host);
			}
		}
	};
};
var TscChecker = class extends Checker {
	constructor() {
		super({
			name: "typescript",
			absFilePath: __filename,
			build: { buildBin: (config) => {
				if (typeof config.typescript === "object") {
					const { root = "", tsconfigPath = "", buildMode } = config.typescript;
					const args = [buildMode ? "-b" : "--noEmit"];
					let projectPath = "";
					if (root || tsconfigPath) projectPath = root ? path.join(root, tsconfigPath) : tsconfigPath;
					if (projectPath) if (buildMode) args.push(projectPath);
					else args.push("-p", projectPath);
					return ["tsc", args];
				}
				return ["tsc", ["--noEmit"]];
			} },
			createDiagnostic
		});
	}
	init() {
		createServeAndBuild = super.initMainThread();
		super.initWorkerThread();
	}
};
const tscChecker = new TscChecker();
tscChecker.prepare();
tscChecker.init();
//#endregion
export { TscChecker, createServeAndBuild };

//# sourceMappingURL=main.js.map