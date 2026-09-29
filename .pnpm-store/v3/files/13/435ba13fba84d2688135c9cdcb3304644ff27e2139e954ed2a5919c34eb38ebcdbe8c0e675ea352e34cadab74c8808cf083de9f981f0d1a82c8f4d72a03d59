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
  BiomeChecker: () => BiomeChecker,
  createServeAndBuild: () => createServeAndBuild
});
module.exports = __toCommonJS(main_exports);
var getImportMetaUrl = () => typeof document === "undefined" ? new URL("file:" + __filename).href : document.currentScript && document.currentScript.src || new URL("main.js", document.baseURI).href;
var importMetaUrl = /* @__PURE__ */ getImportMetaUrl();
var import_node_path = __toESM(require("node:path"), 1);
var import_node_url = require("node:url");
var import_node_worker_threads = require("node:worker_threads");
var import_chokidar = __toESM(require("chokidar"), 1);
var import_Checker = require("../../Checker.js");
var import_FileDiagnosticManager = require("../../FileDiagnosticManager.js");
var import_logger = require("../../logger.js");
var import_types = require("../../types.js");
var import_cli = require("./cli.js");
const __filename2 = (0, import_node_url.fileURLToPath)(importMetaUrl);
const manager = new import_FileDiagnosticManager.FileDiagnosticManager();
let createServeAndBuild;
const createDiagnostic = (pluginConfig) => {
  var _a, _b;
  const biomeConfig = pluginConfig.biome;
  let overlay = true;
  let terminal = true;
  let command = "lint";
  let flags = "";
  if (typeof biomeConfig === "object") {
    command = ((_a = biomeConfig == null ? void 0 : biomeConfig.dev) == null ? void 0 : _a.command) || (biomeConfig == null ? void 0 : biomeConfig.command) || "lint";
    flags = ((_b = biomeConfig == null ? void 0 : biomeConfig.dev) == null ? void 0 : _b.flags) || (biomeConfig == null ? void 0 : biomeConfig.flags) || "";
  }
  return {
    config: async ({ enableOverlay, enableTerminal }) => {
      overlay = enableOverlay;
      terminal = enableTerminal;
    },
    async configureServer({ root }) {
      if (!biomeConfig)
        return;
      const logLevel = (() => {
        var _a2;
        if (typeof biomeConfig !== "object")
          return void 0;
        const userLogLevel = (_a2 = biomeConfig.dev) == null ? void 0 : _a2.logLevel;
        if (!userLogLevel)
          return void 0;
        return userLogLevel.map((l) => import_cli.severityMap[l]);
      })();
      const dispatchDiagnostics = () => {
        var _a2;
        const diagnostics2 = (0, import_logger.filterLogLevel)(manager.getDiagnostics(), logLevel);
        if (terminal) {
          for (const d of diagnostics2) {
            (0, import_logger.consoleLog)((0, import_logger.diagnosticToTerminalLog)(d, "Biome"));
          }
          const errorCount = diagnostics2.filter(
            (d) => d.level === import_types.DiagnosticLevel.Error
          ).length;
          const warningCount = diagnostics2.filter(
            (d) => d.level === import_types.DiagnosticLevel.Warning
          ).length;
          (0, import_logger.consoleLog)((0, import_logger.composeCheckerSummary)("Biome", errorCount, warningCount));
        }
        if (overlay) {
          (_a2 = import_node_worker_threads.parentPort) == null ? void 0 : _a2.postMessage({
            type: import_types.ACTION_TYPES.overlayError,
            payload: (0, import_logger.toClientPayload)(
              "biome",
              diagnostics2.map((d) => (0, import_logger.diagnosticToRuntimeError)(d))
            )
          });
        }
      };
      const handleFileChange = async (filePath, type) => {
        const absPath = import_node_path.default.resolve(root, filePath);
        if (type === "unlink") {
          manager.updateByFileId(absPath, []);
        } else if (type === "change") {
          const isConfigFile = import_node_path.default.basename(absPath) === "biome.json";
          if (isConfigFile) {
            const runCommand2 = (0, import_cli.getBiomeCommand)(command, flags, root);
            const diagnostics2 = await (0, import_cli.runBiome)(runCommand2, root);
            manager.initWith(diagnostics2);
          } else {
            const runCommand2 = (0, import_cli.getBiomeCommand)(command, flags, absPath);
            const diagnosticsOfChangedFile = await (0, import_cli.runBiome)(runCommand2, root);
            manager.updateByFileId(absPath, diagnosticsOfChangedFile);
          }
        }
        dispatchDiagnostics();
      };
      const runCommand = (0, import_cli.getBiomeCommand)(command, flags, root);
      const diagnostics = await (0, import_cli.runBiome)(runCommand, root);
      manager.initWith(diagnostics);
      dispatchDiagnostics();
      const watcher = import_chokidar.default.watch([], {
        cwd: root,
        ignored: (path2) => path2.includes("node_modules")
      });
      watcher.on("change", async (filePath) => {
        handleFileChange(filePath, "change");
      });
      watcher.on("unlink", async (filePath) => {
        handleFileChange(filePath, "unlink");
      });
      watcher.add(".");
    }
  };
};
class BiomeChecker extends import_Checker.Checker {
  constructor() {
    super({
      name: "biome",
      absFilePath: __filename2,
      build: {
        buildBin: (pluginConfig) => {
          if (typeof pluginConfig.biome === "object") {
            const { command, flags } = pluginConfig.biome;
            return ["biome", [command || "check", flags || ""]];
          }
          return ["biome", ["check"]];
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
const biomeChecker = new BiomeChecker();
biomeChecker.prepare();
biomeChecker.init();
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  BiomeChecker,
  createServeAndBuild
});
//# sourceMappingURL=main.js.map