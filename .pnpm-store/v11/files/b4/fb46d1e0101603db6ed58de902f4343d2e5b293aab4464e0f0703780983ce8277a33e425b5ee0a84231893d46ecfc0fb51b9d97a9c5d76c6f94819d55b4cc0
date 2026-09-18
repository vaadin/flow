import { BuildInCheckerNames, BuildInCheckers, CreateDiagnostic, ServeAndBuildChecker } from "./types.js";
import { Script } from "./worker.js";

//#region src/Checker.d.ts
interface CheckerMeta<T extends BuildInCheckerNames> {
  name: T;
  absFilePath: string;
  createDiagnostic: CreateDiagnostic<T>;
  build: ServeAndBuildChecker['build'];
  script?: Script<any>;
}
declare abstract class Checker<T extends BuildInCheckerNames> implements CheckerMeta<T> {
  static logger: ((...v: string[]) => unknown)[];
  static log(...args: any[]): void;
  name: T;
  absFilePath: string;
  createDiagnostic: CreateDiagnostic<T>;
  build: ServeAndBuildChecker['build'];
  script?: Script<any>;
  constructor({
    name,
    absFilePath,
    createDiagnostic,
    build
  }: CheckerMeta<T>);
  prepare(): Script<Pick<BuildInCheckers, T>>;
  initMainThread(): ((config: any, env: import("vite").ConfigEnv) => ServeAndBuildChecker) | undefined;
  initWorkerThread(): void;
}
//#endregion
export { Checker };
//# sourceMappingURL=Checker.d.ts.map