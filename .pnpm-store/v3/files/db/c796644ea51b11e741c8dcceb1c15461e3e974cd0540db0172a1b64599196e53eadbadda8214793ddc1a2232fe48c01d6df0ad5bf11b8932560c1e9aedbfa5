import { SourceLocation } from '@babel/code-frame';

/**
 * Create a code frame from source code and location
 * @param source source code
 * @param location babel compatible location to highlight
 */
declare function createFrame(source: string, location: SourceLocation): string;
declare function tsLikeLocToBabelLoc(tsLoc: Record<'start' | 'end', {
    line: number;
    character: number;
} /** 0-based */>): SourceLocation;
declare function lineColLocToBabelLoc(d: {
    line: number;
    column: number;
    endLine?: number;
    endColumn?: number;
}): SourceLocation;

export { createFrame, lineColLocToBabelLoc, tsLikeLocToBabelLoc };
