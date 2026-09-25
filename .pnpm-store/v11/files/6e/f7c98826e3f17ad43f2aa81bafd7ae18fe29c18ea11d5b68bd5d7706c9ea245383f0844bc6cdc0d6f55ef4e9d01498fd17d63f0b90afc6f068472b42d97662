import { BuildCheckBin, BuildInCheckers, ServeAndBuildChecker, ServeChecker, SharedConfig } from "./types.js";
import { ConfigEnv } from "vite";

//#region src/worker.d.ts
interface WorkerScriptOptions {
  absFilename: string;
  buildBin: BuildCheckBin;
  serverChecker: ServeChecker;
}
interface Script<T> {
  mainScript: () => (config: T & SharedConfig, env: ConfigEnv) => ServeAndBuildChecker;
  workerScript: () => void;
}
declare function createScript<T extends Partial<BuildInCheckers>>({
  absFilename,
  buildBin,
  serverChecker
}: WorkerScriptOptions): Script<T>;
//#endregion
export { Script, createScript };
//# sourceMappingURL=worker.d.ts.map