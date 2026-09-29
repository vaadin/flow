var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);
var cli_exports = {};
__export(cli_exports, {
  getBiomeCommand: () => getBiomeCommand,
  runBiome: () => runBiome,
  severityMap: () => severityMap
});
module.exports = __toCommonJS(cli_exports);
var import_node_child_process = require("node:child_process");
var import_node_path = __toESM(require("node:path"), 1);
var import_strip_ansi = __toESM(require("strip-ansi"), 1);
var import_codeFrame = require("../../codeFrame.js");
var import_types = require("../../types.js");
const severityMap = {
  error: import_types.DiagnosticLevel.Error,
  warning: import_types.DiagnosticLevel.Warning,
  info: import_types.DiagnosticLevel.Suggestion
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
    (0, import_node_child_process.exec)(
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
      file = import_node_path.default.normalize(file);
    const loc = {
      file: file || "",
      start: getLineAndColumn(d.location.sourceCode, (_b = d.location.span) == null ? void 0 : _b[0]),
      end: getLineAndColumn(d.location.sourceCode, (_c = d.location.span) == null ? void 0 : _c[1])
    };
    const codeFrame = (0, import_codeFrame.createFrame)(d.location.sourceCode || "", loc);
    return {
      message: `[${d.category}] ${d.description}`,
      conclusion: "",
      level: severityMap[d.severity] ?? import_types.DiagnosticLevel.Error,
      checker: "Biome",
      id: file,
      codeFrame,
      stripedCodeFrame: codeFrame && (0, import_strip_ansi.default)(codeFrame),
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
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  getBiomeCommand,
  runBiome,
  severityMap
});
//# sourceMappingURL=cli.js.map