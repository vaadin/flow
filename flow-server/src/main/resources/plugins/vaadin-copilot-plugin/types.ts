import type ts, { Program } from 'typescript';
import { type ParsedCommandLine } from 'typescript';

export interface NodeSource {
  fileName: string;
  lineNumber: number;
  columnNumber: number;
}
export interface TsContext {
  program: Program;
  checker: ts.TypeChecker;
  sourceFile: ts.SourceFile;
  config: ParsedCommandLine;
  filePath: string;
}
