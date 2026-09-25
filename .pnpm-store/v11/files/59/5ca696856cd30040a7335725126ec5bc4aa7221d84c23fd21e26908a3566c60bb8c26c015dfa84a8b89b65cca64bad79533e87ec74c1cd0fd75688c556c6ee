import os from "node:os";
import { codeFrameColumns } from "@babel/code-frame";
//#region src/codeFrame.ts
/**
* Create a code frame from source code and location
* @param source source code
* @param location babel compatible location to highlight
*/
function createFrame(source, location) {
	return codeFrameColumns(source, location, { forceColor: true }).split("\n").map((line) => `  ${line}`).join(os.EOL);
}
function tsLikeLocToBabelLoc(tsLoc) {
	return {
		start: {
			line: tsLoc.start.line + 1,
			column: tsLoc.start.character + 1
		},
		end: {
			line: tsLoc.end.line + 1,
			column: tsLoc.end.character + 1
		}
	};
}
function lineColLocToBabelLoc(d) {
	return {
		start: {
			line: d.line,
			column: d.column
		},
		end: {
			line: d.endLine || 0,
			column: d.endColumn
		}
	};
}
/**
* Convert a [startOffset, length] range into a Babel-compatible SourceLocation.
* - Lines/columns are 1-based.
* - Offsets and length are clamped to source bounds.
* - Single-pass up to the end offset, minimal allocations.
*/
function offsetRangeToBabelLocation(source, offset, length) {
	const defaultPos = {
		line: 1,
		column: 1
	};
	if (!source || source.length === 0) return {
		start: { ...defaultPos },
		end: { ...defaultPos }
	};
	const startIndex = offset;
	const endIndex = offset + length;
	let line = 1;
	let column = 1;
	let start = null;
	for (let i = 0; i < endIndex; i++) {
		if (i === startIndex) start = {
			line,
			column
		};
		if (source[i] === "\n") {
			line++;
			column = 1;
		} else column++;
	}
	start ??= {
		line,
		column
	};
	return {
		start,
		end: {
			line,
			column
		}
	};
}
//#endregion
export { createFrame, lineColLocToBabelLoc, offsetRangeToBabelLocation, tsLikeLocToBabelLoc };

//# sourceMappingURL=codeFrame.js.map