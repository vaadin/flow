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
var main_exports = {};
__export(main_exports, {
  TscChecker: () => TscChecker,
  createServeAndBuild: () => createServeAndBuild
});
module.exports = __toCommonJS(main_exports);
var getImportMetaUrl = () => typeof document === "undefined" ? new URL("file:" + __filename).href : document.currentScript && document.currentScript.src || new URL("main.js", document.baseURI).href;
var importMetaUrl = /* @__PURE__ */ getImportMetaUrl();
var import_node_os = __toESM(require("node:os"), 1);
var import_node_path = __toESM(require("node:path"), 1);
var import_node_url = require("node:url");
var import_node_worker_threads = require("node:worker_threads");
var import_chalk = __toESM(require("chalk"), 1);
var import_tiny_invariant = __toESM(require("tiny-invariant"), 1);
var import_typescript = __toESM(require("typescript"), 1);
var import_Checker = require("../../Checker.js");
var import_logger = require("../../logger.js");
var import_types = require("../../types.js");
const __filename2 = (0, import_node_url.fileURLToPath)(importMetaUrl);
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
    configureServer({ root }) {
      (0, import_tiny_invariant.default)(pluginConfig.typescript, "config.typescript should be `false`");
      const finalConfig = pluginConfig.typescript === true ? { root, tsconfigPath: "tsconfig.json" } : {
        root: pluginConfig.typescript.root ?? root,
        tsconfigPath: pluginConfig.typescript.tsconfigPath ?? "tsconfig.json"
      };
      let configFile;
      configFile = import_typescript.default.findConfigFile(
        finalConfig.root,
        import_typescript.default.sys.fileExists,
        finalConfig.tsconfigPath
      );
      if (configFile === void 0) {
        throw Error(
          `Failed to find a valid tsconfig.json: ${finalConfig.tsconfigPath} at ${finalConfig.root} is not a valid tsconfig`
        );
      }
      let logChunk = "";
      const reportDiagnostic = (diagnostic) => {
        const normalizedDiagnostic = (0, import_logger.normalizeTsDiagnostic)(diagnostic);
        if (normalizedDiagnostic === null) {
          return;
        }
        currDiagnostics.push((0, import_logger.diagnosticToRuntimeError)(normalizedDiagnostic));
        logChunk += import_node_os.default.EOL + (0, import_logger.diagnosticToTerminalLog)(normalizedDiagnostic, "TypeScript");
      };
      const reportWatchStatusChanged = (diagnostic, newLine, options, errorCount) => {
        var _a;
        if (diagnostic.code === 6031)
          return;
        switch (diagnostic.code) {
          case 6031:
          case 6032:
            logChunk = "";
            currDiagnostics = [];
            return;
          case 6193:
          case 6194:
            if (overlay) {
              (_a = import_node_worker_threads.parentPort) == null ? void 0 : _a.postMessage({
                type: import_types.ACTION_TYPES.overlayError,
                payload: (0, import_logger.toClientPayload)("typescript", currDiagnostics)
              });
            }
        }
        (0, import_logger.ensureCall)(() => {
          if (errorCount === 0) {
            logChunk = "";
          }
          if (terminal) {
            const color = errorCount && errorCount > 0 ? "red" : "green";
            (0, import_logger.consoleLog)(
              import_chalk.default[color](
                logChunk + import_node_os.default.EOL + (0, import_logger.wrapCheckerSummary)(
                  "TypeScript",
                  diagnostic.messageText.toString()
                )
              )
            );
          }
        });
      };
      const createProgram = import_typescript.default.createEmitAndSemanticDiagnosticsBuilderProgram;
      if (typeof pluginConfig.typescript === "object" && pluginConfig.typescript.buildMode) {
        const host = import_typescript.default.createSolutionBuilderWithWatchHost(
          import_typescript.default.sys,
          createProgram,
          reportDiagnostic,
          void 0,
          reportWatchStatusChanged
        );
        import_typescript.default.createSolutionBuilderWithWatch(host, [configFile], {}).build();
      } else {
        const host = import_typescript.default.createWatchCompilerHost(
          configFile,
          { noEmit: true },
          import_typescript.default.sys,
          createProgram,
          reportDiagnostic,
          reportWatchStatusChanged
        );
        import_typescript.default.createWatchProgram(host);
      }
    }
  };
};
class TscChecker extends import_Checker.Checker {
  constructor() {
    super({
      name: "typescript",
      absFilePath: __filename2,
      build: {
        buildBin: (config) => {
          if (typeof config.typescript === "object") {
            const {
              root = "",
              tsconfigPath = "",
              buildMode
            } = config.typescript;
            const args = [buildMode ? "-b" : "--noEmit"];
            let projectPath = "";
            if (root || tsconfigPath) {
              projectPath = root ? import_node_path.default.join(root, tsconfigPath) : tsconfigPath;
            }
            if (projectPath) {
              if (buildMode) {
                args.push(projectPath);
              } else {
                args.push("-p", projectPath);
              }
            }
            return ["tsc", args];
          }
          return ["tsc", ["--noEmit"]];
        }
      },
      createDiagnostic
    });
  }
  init() {
    const _createServeAndBuild = super.initMainThread();
    createServeAndBuild = _createServeAndBuild;
    super.initWorkerThread();
  }
}
const tscChecker = new TscChecker();
tscChecker.prepare();
tscChecker.init();
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  TscChecker,
  createServeAndBuild
});
//# sourceMappingURL=main.js.map