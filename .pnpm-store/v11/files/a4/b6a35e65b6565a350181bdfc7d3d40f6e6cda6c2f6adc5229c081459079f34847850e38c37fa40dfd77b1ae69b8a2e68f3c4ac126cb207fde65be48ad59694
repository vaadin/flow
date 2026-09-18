/**
 * @license
 * Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
import { ResolvedUrl } from './url';
/**
 * Describes a range of text within a source file.
 *
 * NOTE: `line` and `column` Position properties are indexed from zero. Consider
 * displaying them to users as one-indexed numbers to match text editor
 * conventions.
 */
export interface SourceRange {
    readonly file: ResolvedUrl;
    readonly start: SourcePosition;
    readonly end: SourcePosition;
}
export interface SourcePosition {
    /** The line number, starting from zero. */
    readonly line: number;
    /** The column offset within the line, starting from zero. */
    readonly column: number;
}
export interface LocationOffset {
    /** Zero based line index. */
    readonly line: number;
    /** Zero based column index. */
    readonly col: number;
    /**
     * The url of the source file.
     */
    readonly filename?: ResolvedUrl;
}
/**
 * Corrects source ranges based on an offset.
 *
 * Source ranges for inline documents need to be corrected relative to their
 * positions in their containing documents.
 *
 * For example, if a <script> tag appears in the fifth line of its containing
 * document, we need to move all the source ranges inside that script tag down
 * by 5 lines. We also need to correct the column offsets, but only for the
 * first line of the <script> contents.
 */
export declare function correctSourceRange(sourceRange: SourceRange, locationOffset?: LocationOffset | null): SourceRange;
export declare function correctSourceRange(sourceRange: undefined, locationOffset?: LocationOffset | null): undefined;
export declare function correctSourceRange(sourceRange?: SourceRange, locationOffset?: LocationOffset | null): SourceRange | undefined;
export declare function correctPosition(position: SourcePosition, locationOffset: LocationOffset): SourcePosition;
export declare function uncorrectSourceRange(sourceRange?: SourceRange, locationOffset?: LocationOffset | null): SourceRange | undefined;
export declare function uncorrectPosition(position: SourcePosition, locationOffset: LocationOffset): SourcePosition;
/**
 * Returns -1 if source position `a` comes before source position `b`, returns 0
 * if they are the same, returns 1 if `a` comes after `b`.
 */
export declare function comparePosition(a: SourcePosition, b: SourcePosition): number;
/**
 * If the position is inside the range, returns 0. If it comes before the range,
 * it returns -1. If it comes after the range, it returns 1.
 *
 * TODO(rictic): test this method directly (currently most of its tests are
 *   indirectly, through ast-from-source-position).
 */
export declare function comparePositionAndRange(position: SourcePosition, range: SourceRange, includeEdges?: boolean): 1 | -1 | 0;
export declare function isPositionInsideRange(position: SourcePosition, range: SourceRange | undefined, includeEdges?: boolean): boolean;
