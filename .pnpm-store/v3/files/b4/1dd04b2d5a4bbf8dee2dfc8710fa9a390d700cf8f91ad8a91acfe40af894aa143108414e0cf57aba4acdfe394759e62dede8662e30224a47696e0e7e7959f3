import os from "node:os";
import { codeFrameColumns } from "@babel/code-frame";
function createFrame(source, location) {
  return codeFrameColumns(source, location, {
    // worker tty did not fork parent process stdout, let's make a workaround
    forceColor: true
  }).split("\n").map((line) => `  ${line}`).join(os.EOL);
}
function tsLikeLocToBabelLoc(tsLoc) {
  return {
    start: { line: tsLoc.start.line + 1, column: tsLoc.start.character + 1 },
    end: { line: tsLoc.end.line + 1, column: tsLoc.end.character + 1 }
  };
}
function lineColLocToBabelLoc(d) {
  return {
    start: { line: d.line, column: d.column },
    end: { line: d.endLine || 0, column: d.endColumn }
  };
}
export {
  createFrame,
  lineColLocToBabelLoc,
  tsLikeLocToBabelLoc
};
//# sourceMappingURL=codeFrame.js.map