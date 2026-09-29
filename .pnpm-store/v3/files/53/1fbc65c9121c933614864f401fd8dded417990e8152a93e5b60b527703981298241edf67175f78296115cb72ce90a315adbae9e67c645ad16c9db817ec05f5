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
var logger_exports = {};
__export(logger_exports, {
  composeCheckerSummary: () => composeCheckerSummary,
  consoleLog: () => consoleLog,
  diagnosticToRuntimeError: () => diagnosticToRuntimeError,
  diagnosticToTerminalLog: () => diagnosticToTerminalLog,
  ensureCall: () => ensureCall,
  filterLogLevel: () => filterLogLevel,
  normalizeEslintDiagnostic: () => normalizeEslintDiagnostic,
  normalizeLspDiagnostic: () => normalizeLspDiagnostic,
  normalizePublishDiagnosticParams: () => normalizePublishDiagnosticParams,
  normalizeStylelintDiagnostic: () => normalizeStylelintDiagnostic,
  normalizeTsDiagnostic: () => normalizeTsDiagnostic,
  normalizeVueTscDiagnostic: () => normalizeVueTscDiagnostic,
  toClientPayload: () => toClientPayload,
  uriToAbsPath: () => uriToAbsPath,
  wrapCheckerSummary: () => wrapCheckerSummary
});
module.exports = __toCommonJS(logger_exports);
var getImportMetaUrl = () => typeof document === "undefined" ? new URL("file:" + __filename).href : document.currentScript && document.currentScript.src || new URL("main.js", document.baseURI).href;
var importMetaUrl = /* @__PURE__ */ getImportMetaUrl();
var import_node_fs = __toESM(require("node:fs"), 1);
var import_node_module = require("node:module");
var import_node_os = __toESM(require("node:os"), 1);
var import_chalk = __toESM(require("chalk"), 1);
var import_strip_ansi = __toESM(require("strip-ansi"), 1);
var _vscodeUri = __toESM(require("vscode-uri"), 1);
var import_node_worker_threads = require("node:worker_threads");
var import_client = require("./client/index.js");
var import_codeFrame = require("./codeFrame.js");
var import_types = require("./types.js");
var import_utils = require("./utils.js");
var _a;
const URI = ((_a = _vscodeUri == null ? void 0 : _vscodeUri.default) == null ? void 0 : _a.URI) ?? _vscodeUri.URI;
const _require = (0, import_node_module.createRequire)(importMetaUrl);
const defaultLogLevel = [
  import_types.DiagnosticLevel.Warning,
  import_types.DiagnosticLevel.Error,
  import_types.DiagnosticLevel.Suggestion,
  import_types.DiagnosticLevel.Message
];
function filterLogLevel(diagnostics, level = defaultLogLevel) {
  if (Array.isArray(diagnostics)) {
    return diagnostics.filter((d) => {
      if (typeof d.level !== "number")
        return false;
      return level.includes(d.level);
    });
  }
  if (!diagnostics.level)
    return null;
  return level.includes(diagnostics.level) ? diagnostics : null;
}
function diagnosticToTerminalLog(d, name) {
  const nameInLabel = name ? `(${name})` : "";
  const boldBlack = import_chalk.default.bold.rgb(0, 0, 0);
  const labelMap = {
    [import_types.DiagnosticLevel.Error]: boldBlack.bgRedBright(` ERROR${nameInLabel} `),
    [import_types.DiagnosticLevel.Warning]: boldBlack.bgYellowBright(
      ` WARNING${nameInLabel} `
    ),
    [import_types.DiagnosticLevel.Suggestion]: boldBlack.bgBlueBright(
      ` SUGGESTION${nameInLabel} `
    ),
    [import_types.DiagnosticLevel.Message]: boldBlack.bgCyanBright(
      ` MESSAGE${nameInLabel} `
    )
  };
  const levelLabel = labelMap[d.level ?? import_types.DiagnosticLevel.Error];
  const fileLabel = `${boldBlack.bgCyanBright(" FILE ")} `;
  const position = d.loc ? `${import_chalk.default.yellow(d.loc.start.line)}:${import_chalk.default.yellow(d.loc.start.column)}` : "";
  return [
    `${levelLabel} ${d.message}`,
    `${fileLabel + d.id}:${position}${import_node_os.default.EOL}`,
    d.codeFrame + import_node_os.default.EOL,
    d.conclusion
  ].filter(Boolean).join(import_node_os.default.EOL);
}
function diagnosticToRuntimeError(diagnostics) {
  const diagnosticsArray = Array.isArray(diagnostics) ? diagnostics : [diagnostics];
  const results = diagnosticsArray.map((d) => {
    let loc;
    if (d.loc) {
      loc = {
        file: d.id ?? "",
        line: d.loc.start.line,
        column: typeof d.loc.start.column === "number" ? d.loc.start.column : 0
      };
    }
    return {
      message: d.message ?? "",
      stack: typeof d.stack === "string" ? d.stack : Array.isArray(d.stack) ? d.stack.join(import_node_os.default.EOL) : "",
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
    event: import_client.WS_CHECKER_ERROR_EVENT,
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
  const hasError = errorCount > 0;
  const hasWarning = warningCount > 0;
  const color = hasError ? "red" : hasWarning ? "yellow" : "green";
  return import_chalk.default[color](wrapCheckerSummary(checkerName, message));
}
function normalizeTsDiagnostic(d) {
  var _a2, _b, _c;
  const fileName = (_a2 = d.file) == null ? void 0 : _a2.fileName;
  const {
    flattenDiagnosticMessageText
  } = _require("typescript");
  const message = flattenDiagnosticMessageText(d.messageText, import_node_os.default.EOL);
  let loc;
  const pos = d.start === void 0 ? null : (_c = (_b = d.file) == null ? void 0 : _b.getLineAndCharacterOfPosition) == null ? void 0 : _c.call(_b, d.start);
  if (pos && d.file && typeof d.start === "number" && typeof d.length === "number") {
    loc = (0, import_codeFrame.tsLikeLocToBabelLoc)({
      start: pos,
      end: d.file.getLineAndCharacterOfPosition(d.start + d.length)
    });
  }
  let codeFrame;
  if (loc) {
    codeFrame = (0, import_codeFrame.createFrame)(d.file.text, loc);
  }
  return {
    message,
    conclusion: "",
    codeFrame,
    stripedCodeFrame: codeFrame && (0, import_strip_ansi.default)(codeFrame),
    id: fileName,
    checker: "TypeScript",
    loc,
    level: d.category
  };
}
function normalizeLspDiagnostic({
  diagnostic,
  absFilePath,
  fileText
}) {
  let level = import_types.DiagnosticLevel.Error;
  const loc = (0, import_codeFrame.tsLikeLocToBabelLoc)(diagnostic.range);
  const codeFrame = (0, import_codeFrame.createFrame)(fileText, loc);
  switch (diagnostic.severity) {
    case 1:
      level = import_types.DiagnosticLevel.Error;
      break;
    case 2:
      level = import_types.DiagnosticLevel.Warning;
      break;
    case 3:
      level = import_types.DiagnosticLevel.Message;
      break;
    case 4:
      level = import_types.DiagnosticLevel.Suggestion;
      break;
  }
  return {
    message: diagnostic.message.trim(),
    conclusion: "",
    codeFrame,
    stripedCodeFrame: codeFrame && (0, import_strip_ansi.default)(codeFrame),
    id: absFilePath,
    checker: "VLS",
    loc,
    level
  };
}
async function normalizePublishDiagnosticParams(publishDiagnostics) {
  const diagnostics = publishDiagnostics.diagnostics;
  const absFilePath = uriToAbsPath(publishDiagnostics.uri);
  const { readFile } = import_node_fs.default.promises;
  const fileText = await readFile(absFilePath, "utf-8");
  const res = diagnostics.map((d) => {
    return normalizeLspDiagnostic({
      diagnostic: d,
      absFilePath,
      fileText
    });
  });
  return res;
}
function uriToAbsPath(documentUri) {
  return URI.parse(documentUri).fsPath;
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
    let level = import_types.DiagnosticLevel.Error;
    switch (d.severity) {
      case 0:
        level = import_types.DiagnosticLevel.Error;
        return null;
      case 1:
        level = import_types.DiagnosticLevel.Warning;
        break;
      case 2:
        level = import_types.DiagnosticLevel.Error;
        break;
    }
    const loc = (0, import_codeFrame.lineColLocToBabelLoc)(d);
    const codeFrame = (0, import_codeFrame.createFrame)(diagnostic.source ?? "", loc);
    return {
      message: `${d.message} (${d.ruleId})`,
      conclusion: "",
      codeFrame,
      stripedCodeFrame: codeFrame && (0, import_strip_ansi.default)(codeFrame),
      id: diagnostic.filePath,
      checker: "ESLint",
      loc,
      level
    };
  }).filter(isNormalizedDiagnostic);
}
function normalizeStylelintDiagnostic(diagnostic) {
  return diagnostic.warnings.map((d) => {
    let level = import_types.DiagnosticLevel.Error;
    switch (d.severity) {
      case "warning":
        level = import_types.DiagnosticLevel.Warning;
        break;
      case "error":
        level = import_types.DiagnosticLevel.Error;
        break;
      default:
        level = import_types.DiagnosticLevel.Error;
        return null;
    }
    const loc = (0, import_codeFrame.lineColLocToBabelLoc)(d);
    const codeFrame = (0, import_codeFrame.createFrame)(
      // @ts-ignore
      diagnostic._postcssResult.css ?? "",
      loc
    );
    return {
      message: `${d.text} (${d.rule})`,
      conclusion: "",
      codeFrame,
      stripedCodeFrame: codeFrame && (0, import_strip_ansi.default)(codeFrame),
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
function consoleLog(value) {
  var _a2;
  if (import_utils.isMainThread) {
    console.log(value);
  } else {
    (_a2 = import_node_worker_threads.parentPort) == null ? void 0 : _a2.postMessage({
      type: import_types.ACTION_TYPES.console,
      payload: value
    });
  }
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  composeCheckerSummary,
  consoleLog,
  diagnosticToRuntimeError,
  diagnosticToTerminalLog,
  ensureCall,
  filterLogLevel,
  normalizeEslintDiagnostic,
  normalizeLspDiagnostic,
  normalizePublishDiagnosticParams,
  normalizeStylelintDiagnostic,
  normalizeTsDiagnostic,
  normalizeVueTscDiagnostic,
  toClientPayload,
  uriToAbsPath,
  wrapCheckerSummary
});
//# sourceMappingURL=logger.js.map