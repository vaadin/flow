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
  checker: () => checker,
  default: () => main_default
});
module.exports = __toCommonJS(main_exports);
var import_node_child_process = require("node:child_process");
var import_chalk = __toESM(require("chalk"), 1);
var import_npm_run_path = __toESM(require("npm-run-path"), 1);
var import_Checker = require("./Checker.js");
var import_client = require("./client/index.js");
var import_types = require("./types.js");
const buildInCheckerKeys = [
  "typescript",
  "vueTsc",
  "vls",
  "eslint",
  "stylelint",
  "biome"
];
async function createCheckers(userConfig, env) {
  const serveAndBuildCheckers = [];
  const { enableBuild, overlay } = userConfig;
  const sharedConfig = { enableBuild, overlay };
  for (const name of buildInCheckerKeys) {
    if (!userConfig[name])
      continue;
    const { createServeAndBuild } = await import(`./checkers/${name}/main.js`);
    serveAndBuildCheckers.push(
      createServeAndBuild({ [name]: userConfig[name], ...sharedConfig }, env)
    );
  }
  return serveAndBuildCheckers;
}
function checker(userConfig) {
  const enableBuild = (userConfig == null ? void 0 : userConfig.enableBuild) ?? true;
  const enableOverlay = (userConfig == null ? void 0 : userConfig.overlay) !== false;
  const enableTerminal = (userConfig == null ? void 0 : userConfig.terminal) !== false;
  const overlayConfig = typeof (userConfig == null ? void 0 : userConfig.overlay) === "object" ? userConfig == null ? void 0 : userConfig.overlay : {};
  let initialized = false;
  let initializeCounter = 0;
  let checkers = [];
  let isProduction = false;
  let baseWithOrigin;
  let viteMode;
  let buildWatch = false;
  let logger = null;
  return {
    name: "vite-plugin-checker",
    enforce: "pre",
    // @ts-ignore
    __internal__checker: import_Checker.Checker,
    config: async (config, env) => {
      viteMode = env.command;
      if (initializeCounter === 0) {
        initializeCounter++;
      } else {
        initialized = true;
        return;
      }
      checkers = await createCheckers(userConfig || {}, env);
      if (viteMode !== "serve")
        return;
      for (const checker2 of checkers) {
        const workerConfig = checker2.serve.config;
        workerConfig({
          enableOverlay,
          enableTerminal,
          env
        });
      }
    },
    configResolved(config) {
      logger = config.logger;
      baseWithOrigin = config.server.origin ? config.server.origin + config.base : config.base;
      isProduction || (isProduction = config.isProduction || config.command === "build");
      buildWatch = !!config.build.watch;
    },
    buildEnd() {
      if (initialized)
        return;
      if (viteMode === "serve") {
        for (const checker2 of checkers) {
          const { worker } = checker2.serve;
          worker.terminate();
        }
      }
    },
    resolveId(id) {
      if (id === import_client.RUNTIME_CLIENT_RUNTIME_PATH || id === import_client.RUNTIME_CLIENT_ENTRY_PATH) {
        return (0, import_client.wrapVirtualPrefix)(id);
      }
      return;
    },
    load(id) {
      if (id === (0, import_client.wrapVirtualPrefix)(import_client.RUNTIME_CLIENT_RUNTIME_PATH)) {
        return import_client.runtimeCode;
      }
      if (id === (0, import_client.wrapVirtualPrefix)(import_client.RUNTIME_CLIENT_ENTRY_PATH)) {
        return (0, import_client.composePreambleCode)({ baseWithOrigin, overlayConfig });
      }
      return;
    },
    transformIndexHtml() {
      if (initialized)
        return;
      if (isProduction)
        return;
      return [
        {
          tag: "script",
          attrs: { type: "module" },
          children: (0, import_client.composePreambleCode)({ baseWithOrigin, overlayConfig })
        }
      ];
    },
    buildStart: () => {
      if (initialized)
        return;
      if (!isProduction || !enableBuild)
        return;
      const localEnv = import_npm_run_path.default.env({
        env: process.env,
        cwd: process.cwd(),
        execPath: process.execPath
      });
      (async () => {
        const exitCodes = await Promise.all(
          checkers.map(
            (checker2) => spawnChecker(checker2, userConfig, localEnv)
          )
        );
        const exitCode = exitCodes.find((code) => code !== 0) ?? 0;
        if (exitCode !== 0 && !buildWatch) {
          process.exit(exitCode);
        }
      })();
    },
    configureServer(server) {
      if (initialized)
        return;
      const latestOverlayErrors = new Array(
        checkers.length
      );
      checkers.forEach((checker2, index) => {
        const { worker, configureServer: workerConfigureServer } = checker2.serve;
        workerConfigureServer({ root: userConfig.root || server.config.root });
        worker.on("message", (action) => {
          if (action.type === import_types.ACTION_TYPES.overlayError) {
            latestOverlayErrors[index] = action.payload;
            if (action.payload) {
              server.ws.send("vite-plugin-checker", action.payload);
            }
          } else if (action.type === import_types.ACTION_TYPES.console) {
            if (import_Checker.Checker.logger.length) {
              import_Checker.Checker.log(action);
            } else {
              logger.error(action.payload);
            }
          }
        });
      });
      if (server.ws.on) {
        server.watcher.on("change", () => {
          logger.clearScreen("error");
        });
        server.ws.on("vite-plugin-checker", (data) => {
          if (data.event === "runtime-loaded") {
            server.ws.send("vite-plugin-checker", {
              event: import_client.WS_CHECKER_RECONNECT_EVENT,
              data: latestOverlayErrors.filter(Boolean)
            });
          }
        });
      } else {
        setTimeout(() => {
          logger.warn(
            import_chalk.default.yellow(
              "[vite-plugin-checker]: `server.ws.on` is introduced to Vite in 2.6.8, see [PR](https://github.com/vitejs/vite/pull/5273) and [changelog](https://github.com/vitejs/vite/blob/main/packages/vite/CHANGELOG.md#268-2021-10-18). \nvite-plugin-checker relies on `server.ws.on` to send overlay message to client. Support for Vite < 2.6.8 will be removed in the next major version release."
            )
          );
        }, 5e3);
      }
    }
  };
}
function spawnChecker(checker2, userConfig, localEnv) {
  return new Promise((resolve) => {
    const buildBin = checker2.build.buildBin;
    const finalBin = typeof buildBin === "function" ? buildBin(userConfig) : buildBin;
    const proc = (0, import_node_child_process.spawn)(...finalBin, {
      cwd: process.cwd(),
      stdio: "inherit",
      env: localEnv,
      // shell is necessary on windows to get the process to even start.
      // Command line args constructed by checkers therefore need to escape double quotes
      // to have them not striped out by cmd.exe. Using shell on all platforms lets us avoid
      // having to perform platform-specific logic around escaping quotes since all platform
      // shells will strip out unescaped double quotes. E.g. shell=false on linux only would
      // result in escaped quotes not being unescaped.
      shell: true
    });
    proc.on("exit", (code) => {
      if (code !== null && code !== 0) {
        resolve(code);
      } else {
        resolve(0);
      }
    });
  });
}
var main_default = checker;
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  checker
});
//# sourceMappingURL=main.js.map