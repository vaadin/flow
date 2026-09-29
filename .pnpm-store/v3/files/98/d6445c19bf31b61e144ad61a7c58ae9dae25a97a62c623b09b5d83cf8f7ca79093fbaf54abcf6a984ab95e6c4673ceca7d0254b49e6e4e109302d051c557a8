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
  VueTscChecker: () => VueTscChecker,
  createServeAndBuild: () => createServeAndBuild
});
module.exports = __toCommonJS(main_exports);
var getImportMetaUrl = () => typeof document === "undefined" ? new URL("file:" + __filename).href : document.currentScript && document.currentScript.src || new URL("main.js", document.baseURI).href;
var importMetaUrl = /* @__PURE__ */ getImportMetaUrl();
var import_node_module = require("node:module");
var import_node_os = __toESM(require("node:os"), 1);
var import_node_path = __toESM(require("node:path"), 1);
var import_node_url = require("node:url");
var import_node_worker_threads = require("node:worker_threads");
var import_tiny_invariant = __toESM(require("tiny-invariant"), 1);
var import_Checker = require("../../Checker.js");
var import_logger = require("../../logger.js");
var import_types = require("../../types.js");
var import_prepareVueTsc = require("./prepareVueTsc.js");
const _require = (0, import_node_module.createRequire)(importMetaUrl);
const __filename2 = (0, import_node_url.fileURLToPath)(importMetaUrl);
let createServeAndBuild;
const createDiagnostic = (pluginConfig) => {
  let overlay = true;
  let terminal = true;
  let currDiagnostics = [];
  return {
    config: ({ enableOverlay, enableTerminal }) => {
      overlay = enableOverlay;
      terminal = enableTerminal;
    },
    async configureServer({ root }) {
      (0, import_tiny_invariant.default)(pluginConfig.vueTsc, "config.vueTsc should be `false`");
      const { targetTsDir } = await (0, import_prepareVueTsc.prepareVueTsc)();
      const vueTs = _require(import_node_path.default.resolve(targetTsDir, "lib/typescript.js"));
      const finalConfig = pluginConfig.vueTsc === true ? { root, tsconfigPath: "tsconfig.json" } : {
        root: pluginConfig.vueTsc.root ?? root,
        tsconfigPath: pluginConfig.vueTsc.tsconfigPath ?? "tsconfig.json"
      };
      const configFile = vueTs.findConfigFile(
        finalConfig.root,
        vueTs.sys.fileExists,
        finalConfig.tsconfigPath
      );
      if (configFile === void 0) {
        throw Error(
          `Failed to find a valid tsconfig.json: ${finalConfig.tsconfigPath} at ${finalConfig.root} is not a valid tsconfig`
        );
      }
      let logChunk = "";
      let prevLogChunk = "";
      const reportDiagnostic = (diagnostic) => {
        const normalizedDiagnostic = (0, import_logger.normalizeVueTscDiagnostic)(diagnostic);
        if (normalizedDiagnostic === null) {
          return;
        }
        currDiagnostics.push((0, import_logger.diagnosticToRuntimeError)(normalizedDiagnostic));
        logChunk += import_node_os.default.EOL + (0, import_logger.diagnosticToTerminalLog)(normalizedDiagnostic, "vue-tsc");
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
                payload: (0, import_logger.toClientPayload)("vue-tsc", currDiagnostics)
              });
            }
        }
        (0, import_logger.ensureCall)(() => {
          if (errorCount === 0) {
            logChunk = "";
          }
          if (terminal) {
            logChunk = logChunk + import_node_os.default.EOL + (0, import_logger.wrapCheckerSummary)("vue-tsc", diagnostic.messageText.toString());
            if (logChunk === prevLogChunk) {
              return;
            }
            prevLogChunk = logChunk;
            (0, import_logger.consoleLog)(logChunk);
          }
        });
      };
      const createProgram = vueTs.createSemanticDiagnosticsBuilderProgram;
      const host = vueTs.createWatchCompilerHost(
        configFile,
        { noEmit: true },
        vueTs.sys,
        createProgram,
        reportDiagnostic,
        reportWatchStatusChanged
      );
      vueTs.createWatchProgram(host);
    }
  };
};
class VueTscChecker extends import_Checker.Checker {
  constructor() {
    super({
      name: "vueTsc",
      absFilePath: __filename2,
      build: {
        buildBin: (config) => {
          if (typeof config.vueTsc === "object") {
            const { root = "", tsconfigPath = "" } = config.vueTsc;
            const args = ["--noEmit"];
            let projectPath = "";
            if (root || tsconfigPath) {
              projectPath = root ? import_node_path.default.join(root, tsconfigPath) : tsconfigPath;
            }
            if (projectPath) {
              args.push("-p", projectPath);
            }
            return ["vue-tsc", args];
          }
          return ["vue-tsc", ["--noEmit"]];
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
const tscChecker = new VueTscChecker();
tscChecker.prepare();
tscChecker.init();
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  VueTscChecker,
  createServeAndBuild
});
//# sourceMappingURL=main.js.map