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
  EslintChecker: () => EslintChecker,
  createServeAndBuild: () => createServeAndBuild
});
module.exports = __toCommonJS(main_exports);
var getImportMetaUrl = () => typeof document === "undefined" ? new URL("file:" + __filename).href : document.currentScript && document.currentScript.src || new URL("main.js", document.baseURI).href;
var importMetaUrl = /* @__PURE__ */ getImportMetaUrl();
var import_node_module = __toESM(require("node:module"), 1);
var import_node_path = __toESM(require("node:path"), 1);
var import_node_url = require("node:url");
var import_node_worker_threads = require("node:worker_threads");
var import_chokidar = __toESM(require("chokidar"), 1);
var import_eslint = require("eslint");
var import_tiny_invariant = __toESM(require("tiny-invariant"), 1);
var import_Checker = require("../../Checker.js");
var import_FileDiagnosticManager = require("../../FileDiagnosticManager.js");
var import_logger = require("../../logger.js");
var import_types = require("../../types.js");
var import_cli = require("./cli.js");
var import_options = require("./options.js");
const __filename2 = (0, import_node_url.fileURLToPath)(importMetaUrl);
const require2 = import_node_module.default.createRequire(importMetaUrl);
const manager = new import_FileDiagnosticManager.FileDiagnosticManager();
let createServeAndBuild;
const createDiagnostic = (pluginConfig) => {
  let overlay = true;
  let terminal = true;
  return {
    config: async ({ enableOverlay, enableTerminal }) => {
      overlay = enableOverlay;
      terminal = enableTerminal;
    },
    async configureServer({ root }) {
      var _a;
      if (!pluginConfig.eslint)
        return;
      const options = import_options.options.parse(pluginConfig.eslint.lintCommand);
      (0, import_tiny_invariant.default)(
        !options.fix,
        "Using `--fix` in `config.eslint.lintCommand` is not allowed in vite-plugin-checker, you could using `--fix` with editor."
      );
      const translatedOptions = (0, import_cli.translateOptions)(options);
      const logLevel = (() => {
        var _a2;
        if (typeof pluginConfig.eslint !== "object")
          return void 0;
        const userLogLevel = (_a2 = pluginConfig.eslint.dev) == null ? void 0 : _a2.logLevel;
        if (!userLogLevel)
          return void 0;
        const map = {
          error: import_types.DiagnosticLevel.Error,
          warning: import_types.DiagnosticLevel.Warning
        };
        return userLogLevel.map((l) => map[l]);
      })();
      const eslintOptions = {
        cwd: root,
        ...translatedOptions,
        ...(_a = pluginConfig.eslint.dev) == null ? void 0 : _a.overrideConfig
      };
      let eslint;
      if (pluginConfig.eslint.useFlatConfig) {
        const {
          FlatESLint,
          shouldUseFlatConfig
        } = require2("eslint/use-at-your-own-risk");
        if (shouldUseFlatConfig == null ? void 0 : shouldUseFlatConfig()) {
          eslint = new FlatESLint({
            cwd: root
          });
        } else {
          throw Error(
            "Please upgrade your eslint to latest version to use `useFlatConfig` option."
          );
        }
      } else {
        eslint = new import_eslint.ESLint(eslintOptions);
      }
      const dispatchDiagnostics = () => {
        var _a2;
        const diagnostics2 = (0, import_logger.filterLogLevel)(manager.getDiagnostics(), logLevel);
        if (terminal) {
          for (const d of diagnostics2) {
            (0, import_logger.consoleLog)((0, import_logger.diagnosticToTerminalLog)(d, "ESLint"));
          }
          const errorCount = diagnostics2.filter(
            (d) => d.level === import_types.DiagnosticLevel.Error
          ).length;
          const warningCount = diagnostics2.filter(
            (d) => d.level === import_types.DiagnosticLevel.Warning
          ).length;
          (0, import_logger.consoleLog)((0, import_logger.composeCheckerSummary)("ESLint", errorCount, warningCount));
        }
        if (overlay) {
          (_a2 = import_node_worker_threads.parentPort) == null ? void 0 : _a2.postMessage({
            type: import_types.ACTION_TYPES.overlayError,
            payload: (0, import_logger.toClientPayload)(
              "eslint",
              diagnostics2.map((d) => (0, import_logger.diagnosticToRuntimeError)(d))
            )
          });
        }
      };
      const handleFileChange = async (filePath, type) => {
        const extension = import_node_path.default.extname(filePath);
        const { extensions } = eslintOptions;
        const hasExtensionsConfig = Array.isArray(extensions);
        if (hasExtensionsConfig && !extensions.includes(extension))
          return;
        const isChangedFileIgnored = await eslint.isPathIgnored(filePath);
        if (isChangedFileIgnored)
          return;
        const absPath = import_node_path.default.resolve(root, filePath);
        if (type === "unlink") {
          manager.updateByFileId(absPath, []);
        } else if (type === "change") {
          const diagnosticsOfChangedFile = await eslint.lintFiles(filePath);
          const newDiagnostics = diagnosticsOfChangedFile.flatMap(
            (d) => (0, import_logger.normalizeEslintDiagnostic)(d)
          );
          manager.updateByFileId(absPath, newDiagnostics);
        }
        dispatchDiagnostics();
      };
      const files = options._.slice(1);
      const diagnostics = await eslint.lintFiles(files);
      manager.initWith(diagnostics.flatMap((p) => (0, import_logger.normalizeEslintDiagnostic)(p)));
      dispatchDiagnostics();
      const watcher = import_chokidar.default.watch([], {
        cwd: root,
        ignored: (path2) => path2.includes("node_modules")
      });
      watcher.add(files);
      watcher.on("change", async (filePath) => {
        handleFileChange(filePath, "change");
      });
      watcher.on("unlink", async (filePath) => {
        handleFileChange(filePath, "unlink");
      });
    }
  };
};
class EslintChecker extends import_Checker.Checker {
  constructor() {
    super({
      name: "eslint",
      absFilePath: __filename2,
      build: {
        buildBin: (pluginConfig) => {
          if (pluginConfig.eslint) {
            const { lintCommand } = pluginConfig.eslint;
            return ["eslint", lintCommand.split(" ").slice(1)];
          }
          return ["eslint", [""]];
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
const eslintChecker = new EslintChecker();
eslintChecker.prepare();
eslintChecker.init();
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  EslintChecker,
  createServeAndBuild
});
//# sourceMappingURL=main.js.map