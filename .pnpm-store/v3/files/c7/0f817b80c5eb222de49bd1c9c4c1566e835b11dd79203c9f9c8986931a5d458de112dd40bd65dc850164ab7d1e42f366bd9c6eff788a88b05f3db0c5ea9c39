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
  VlsChecker: () => VlsChecker,
  createDiagnostic: () => createDiagnostic,
  createServeAndBuild: () => createServeAndBuild
});
module.exports = __toCommonJS(main_exports);
var getImportMetaUrl = () => typeof document === "undefined" ? new URL("file:" + __filename).href : document.currentScript && document.currentScript.src || new URL("main.js", document.baseURI).href;
var importMetaUrl = /* @__PURE__ */ getImportMetaUrl();
var import_node_os = __toESM(require("node:os"), 1);
var import_node_url = require("node:url");
var import_node_worker_threads = require("node:worker_threads");
var import_Checker = require("../../Checker.js");
var import_logger = require("../../logger.js");
var import_types = require("../../types.js");
var import_diagnostics = require("./diagnostics.js");
const __filename2 = (0, import_node_url.fileURLToPath)(importMetaUrl);
let createServeAndBuild;
const createDiagnostic = (pluginConfig) => {
  let overlay = true;
  let terminal = true;
  let command;
  return {
    config: ({ enableOverlay, enableTerminal, env }) => {
      overlay = enableOverlay;
      terminal = enableTerminal;
      command = env.command;
    },
    async configureServer({ root }) {
      const workDir = root;
      const onDispatchDiagnosticsSummary = (errorCount, warningCount) => {
        if (!terminal)
          return;
        (0, import_logger.consoleLog)((0, import_logger.composeCheckerSummary)("VLS", errorCount, warningCount));
      };
      const onDispatchDiagnostics = (normalized) => {
        var _a;
        if (overlay && command === "serve") {
          (_a = import_node_worker_threads.parentPort) == null ? void 0 : _a.postMessage({
            type: import_types.ACTION_TYPES.overlayError,
            payload: (0, import_logger.toClientPayload)(
              "vls",
              (0, import_logger.diagnosticToRuntimeError)(normalized)
            )
          });
        }
        if (terminal) {
          (0, import_logger.consoleLog)(
            normalized.map((d) => (0, import_logger.diagnosticToTerminalLog)(d, "VLS")).join(import_node_os.default.EOL)
          );
        }
      };
      const vlsConfig = pluginConfig == null ? void 0 : pluginConfig.vls;
      await (0, import_diagnostics.diagnostics)(workDir, "WARN", {
        onDispatchDiagnostics,
        onDispatchDiagnosticsSummary,
        watch: true,
        verbose: false,
        config: typeof vlsConfig === "object" ? vlsConfig : null
      });
    }
  };
};
class VlsChecker extends import_Checker.Checker {
  constructor() {
    super({
      name: "vls",
      absFilePath: __filename2,
      build: {
        buildBin: (config) => {
          if (typeof config.vls === "object") {
            return [
              "vti",
              [
                "diagnostics",
                // Escape quotes so that the system shell doesn't strip them out:
                `"${JSON.stringify(config.vls).replace(/[\\"]/g, "\\$&")}"`
              ]
            ];
          }
          return ["vti", ["diagnostics"]];
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
const vlsChecker = new VlsChecker();
vlsChecker.prepare();
vlsChecker.init();
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  VlsChecker,
  createDiagnostic,
  createServeAndBuild
});
//# sourceMappingURL=main.js.map