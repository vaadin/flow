import { NormalizedDiagnostic } from '../../logger.js';
import { DiagnosticLevel } from '../../types.js';
import '@babel/code-frame';
import 'eslint';
import 'stylelint';
import 'vscode-languageclient/node';
import 'typescript';
import 'node:worker_threads';
import 'vite';
import '../vls/initParams.js';
import 'vscode-uri';
import 'vscode-languageserver/node';

declare const severityMap: {
    readonly error: DiagnosticLevel.Error;
    readonly warning: DiagnosticLevel.Warning;
    readonly info: DiagnosticLevel.Suggestion;
};
declare function getBiomeCommand(command: string, flags: string, files: string): string;
declare function runBiome(command: string, cwd: string): Promise<NormalizedDiagnostic[]>;

export { getBiomeCommand, runBiome, severityMap };
