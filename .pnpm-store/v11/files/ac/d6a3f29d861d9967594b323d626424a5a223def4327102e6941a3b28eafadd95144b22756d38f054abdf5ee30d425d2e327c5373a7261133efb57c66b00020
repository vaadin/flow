import { DiagnosticLevel } from "../../types.js";
import { NormalizedDiagnostic } from "../../logger.js";

//#region src/checkers/biome/cli.d.ts
declare const severityMap: {
  readonly error: DiagnosticLevel.Error;
  readonly warning: DiagnosticLevel.Warning;
  readonly info: DiagnosticLevel.Suggestion;
  readonly information: DiagnosticLevel.Suggestion;
};
declare function getBiomeCommand(command: string, flags: string, files: string[]): string[];
declare function runBiome(argv: string[], cwd: string): Promise<NormalizedDiagnostic[]>;
//#endregion
export { getBiomeCommand, runBiome, severityMap };
//# sourceMappingURL=cli.d.ts.map