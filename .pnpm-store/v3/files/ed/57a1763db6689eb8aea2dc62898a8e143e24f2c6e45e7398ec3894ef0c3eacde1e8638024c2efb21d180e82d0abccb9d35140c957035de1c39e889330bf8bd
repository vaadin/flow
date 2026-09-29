import { exec } from "node:child_process";
import path from "node:path";
import strip from "strip-ansi";
import { createFrame } from "../../codeFrame.js";
import { DiagnosticLevel } from "../../types.js";
const severityMap = {
  error: DiagnosticLevel.Error,
  warning: DiagnosticLevel.Warning,
  info: DiagnosticLevel.Suggestion
};
function getBiomeCommand(command, flags, files) {
  const defaultFlags = "--reporter json";
  if (flags.includes("--flags")) {
    throw Error(
      `vite-plugin-checker will force append "--reporter json" to the flags in dev mode, please don't use "--flags" in "config.biome.flags".
If you need to customize "--flags" in build mode, please use "config.biome.build.flags" instead.`
    );
  }
  return ["biome", command, flags, defaultFlags, files].filter(Boolean).join(" ");
}
function runBiome(command, cwd) {
  return new Promise((resolve, reject) => {
    exec(
      command,
      {
        cwd
      },
      (error, stdout, stderr) => {
        resolve([...parseBiomeOutput(stdout)]);
      }
    );
  });
}
function parseBiomeOutput(output) {
  let parsed;
  try {
    parsed = JSON.parse(output);
  } catch (e) {
    return [];
  }
  const diagnostics = parsed.diagnostics.map((d) => {
    var _a, _b, _c;
    let file = (_a = d.location.path) == null ? void 0 : _a.file;
    if (file)
      file = path.normalize(file);
    const loc = {
      file: file || "",
      start: getLineAndColumn(d.location.sourceCode, (_b = d.location.span) == null ? void 0 : _b[0]),
      end: getLineAndColumn(d.location.sourceCode, (_c = d.location.span) == null ? void 0 : _c[1])
    };
    const codeFrame = createFrame(d.location.sourceCode || "", loc);
    return {
      message: `[${d.category}] ${d.description}`,
      conclusion: "",
      level: severityMap[d.severity] ?? DiagnosticLevel.Error,
      checker: "Biome",
      id: file,
      codeFrame,
      stripedCodeFrame: codeFrame && strip(codeFrame),
      loc
    };
  });
  return diagnostics;
}
function getLineAndColumn(text, offset) {
  if (!text || !offset)
    return { line: 0, column: 0 };
  let line = 1;
  let column = 1;
  for (let i = 0; i < offset; i++) {
    if (text[i] === "\n") {
      line++;
      column = 1;
    } else {
      column++;
    }
  }
  return { line, column };
}
export {
  getBiomeCommand,
  runBiome,
  severityMap
};
//# sourceMappingURL=cli.js.map