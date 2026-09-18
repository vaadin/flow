import "./types.js";
import { Worker, parentPort, workerData } from "node:worker_threads";
//#region src/worker.ts
function createScript({ absFilename, buildBin, serverChecker }) {
	return {
		mainScript: () => {
			const createWorker = (checkerConfig, env) => {
				const isBuild = env.command === "build";
				const worker = new Worker(absFilename, { workerData: {
					env,
					checkerConfig
				} });
				return {
					worker,
					config: (config) => {
						if (isBuild) return;
						const configAction = {
							type: "config",
							payload: config
						};
						worker.postMessage(configAction);
					},
					configureServer: (serverConfig) => {
						const configureServerAction = {
							type: "configureServer",
							payload: serverConfig
						};
						worker.postMessage(configureServerAction);
					}
				};
			};
			return (config, env) => {
				return {
					serve: createWorker(config, env),
					build: { buildBin }
				};
			};
		},
		workerScript: () => {
			let diagnostic = null;
			if (!parentPort) throw Error("should have parentPort as file runs in worker thread");
			const isBuild = workerData.env.command === "build";
			const port = parentPort.on("message", (action) => {
				switch (action.type) {
					case "config": {
						const checkerConfig = workerData.checkerConfig;
						diagnostic = serverChecker.createDiagnostic(checkerConfig);
						diagnostic.config(action.payload);
						break;
					}
					case "configureServer":
						if (!diagnostic) throw Error("diagnostic should be initialized in `config` hook of Vite");
						diagnostic.configureServer(action.payload);
						break;
					case "unref":
						port.unref();
						break;
				}
			});
			if (isBuild) port.unref();
		}
	};
}
//#endregion
export { createScript };

//# sourceMappingURL=worker.js.map