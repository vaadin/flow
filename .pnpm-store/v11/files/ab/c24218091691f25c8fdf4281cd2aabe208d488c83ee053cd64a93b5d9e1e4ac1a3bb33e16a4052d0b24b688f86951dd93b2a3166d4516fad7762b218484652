import "./types.js";
import { Checker } from "./Checker.js";
import { WS_CHECKER_RECONNECT_EVENT, composePreambleCode, runtimeCode, wrapVirtualPrefix } from "./client/index.js";
import colors from "picocolors";
import { spawn } from "node:child_process";
import { npmRunPathEnv } from "npm-run-path";
//#region src/main.ts
const buildInCheckerKeys = [
	"typescript",
	"vueTsc",
	"eslint",
	"stylelint",
	"biome",
	"oxlint"
];
async function createCheckers(userConfig, env) {
	const serveAndBuildCheckers = [];
	const { enableBuild, overlay } = userConfig;
	const sharedConfig = {
		enableBuild,
		overlay
	};
	for (const name of buildInCheckerKeys) {
		if (!userConfig[name]) continue;
		const { createServeAndBuild } = await import(`./checkers/${name}/main.js`);
		serveAndBuildCheckers.push(createServeAndBuild({
			[name]: userConfig[name],
			...sharedConfig
		}, env));
	}
	return serveAndBuildCheckers;
}
function checker(userConfig) {
	const enableBuild = userConfig?.enableBuild ?? true;
	const enableOverlay = userConfig?.overlay !== false;
	const enableTerminal = userConfig?.terminal !== false;
	const overlayConfig = typeof userConfig?.overlay === "object" ? userConfig?.overlay : {};
	let initialized = false;
	let initializeCounter = 0;
	let checkers = [];
	let isProduction = false;
	let baseWithOrigin;
	let viteMode;
	let buildWatch = false;
	let logger = null;
	let checkerPromise = null;
	return {
		name: "vite-plugin-checker",
		enforce: "pre",
		__internal__checker: Checker,
		config: async (_config, env) => {
			viteMode = env.command;
			if (initializeCounter === 0) initializeCounter++;
			else {
				initialized = true;
				return;
			}
			checkers = await createCheckers(userConfig || {}, env);
			if (viteMode !== "serve") return;
			for (const checker of checkers) {
				const workerConfig = checker.serve.config;
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
			isProduction ||= config.isProduction || config.command === "build";
			buildWatch = !!config.build.watch;
		},
		async buildEnd() {
			if (viteMode !== "serve") await checkerPromise;
			if (initialized) return;
			if (viteMode === "serve") for (const checker of checkers) {
				const { worker } = checker.serve;
				worker.terminate();
			}
		},
		resolveId(id) {
			if (id === "/@vite-plugin-checker-runtime" || id === "/@vite-plugin-checker-runtime-entry") return wrapVirtualPrefix(id);
		},
		load(id) {
			if (id === wrapVirtualPrefix("/@vite-plugin-checker-runtime")) return runtimeCode;
			if (id === wrapVirtualPrefix("/@vite-plugin-checker-runtime-entry")) return composePreambleCode({
				baseWithOrigin,
				overlayConfig,
				useBase: false
			});
		},
		transformIndexHtml() {
			if (initialized) return;
			if (isProduction) return;
			return [{
				tag: "script",
				attrs: { type: "module" },
				children: composePreambleCode({
					baseWithOrigin,
					overlayConfig
				})
			}];
		},
		buildStart: () => {
			if (initialized) return;
			if (!isProduction || !enableBuild) return;
			const localEnv = npmRunPathEnv({
				env: process.env,
				cwd: process.cwd(),
				execPath: process.execPath
			});
			const spawnedCheckers = checkers.map((checker) => spawnChecker(checker, userConfig, localEnv));
			checkerPromise = Promise.all(spawnedCheckers).then((exitCodes) => {
				const exitCode = exitCodes.find((code) => code !== 0) ?? 0;
				if (exitCode !== 0 && !buildWatch) process.exit(exitCode);
			});
		},
		configureServer(server) {
			if (initialized) return;
			const latestOverlayErrors = new Array(checkers.length);
			checkers.forEach((checker, index) => {
				const { worker, configureServer: workerConfigureServer } = checker.serve;
				workerConfigureServer({ root: userConfig.root || server.config.root });
				worker.on("message", (action) => {
					if (action.type === "overlayError") {
						latestOverlayErrors[index] = action.payload;
						if (action.payload) server.ws.send("vite-plugin-checker", action.payload);
					} else if (action.type === "console") if (Checker.logger.length) Checker.log(action);
					else logger[action.level](action.payload);
				});
			});
			if (server.ws.on) {
				server.watcher.on("change", () => {
					logger.clearScreen("error");
				});
				server.ws.on("vite-plugin-checker", (data) => {
					if (data.event === "runtime-loaded") server.ws.send("vite-plugin-checker", {
						event: WS_CHECKER_RECONNECT_EVENT,
						data: latestOverlayErrors.filter(Boolean)
					});
				});
			} else setTimeout(() => {
				logger.warn(colors.yellow("[vite-plugin-checker]: `server.ws.on` is introduced to Vite in 2.6.8, see [PR](https://github.com/vitejs/vite/pull/5273) and [changelog](https://github.com/vitejs/vite/blob/main/packages/vite/CHANGELOG.md#268-2021-10-18). \nvite-plugin-checker relies on `server.ws.on` to send overlay message to client. Support for Vite < 2.6.8 will be removed in the next major version release."));
			}, 5e3);
		}
	};
}
function spawnChecker(checker, userConfig, localEnv) {
	return new Promise((resolve) => {
		const buildBin = checker.build.buildBin;
		const [command, args] = typeof buildBin === "function" ? buildBin(userConfig) : buildBin;
		spawn([command, ...args].join(" "), {
			cwd: process.cwd(),
			stdio: "inherit",
			env: localEnv,
			shell: true
		}).on("exit", (code) => {
			if (code !== null && code !== 0) resolve(code);
			else resolve(0);
		});
	});
}
//#endregion
export { checker, checker as default };

//# sourceMappingURL=main.js.map