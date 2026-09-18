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
import { AstNodeWithLanguage } from '../model/inline-document';
import { LocationOffset, SourcePosition, SourceRange } from '../model/source-range';
import { ResolvedUrl } from '../model/url';
/**
 * A parsed Document.
 *
 * @template AstNode The AST type of the document.
 * @template Visitor The type of the visitors that can walk the document.
 */
export declare abstract class ParsedDocument<AstNode = {} | null | undefined, Visitor = {}> {
    abstract type: string;
    url: ResolvedUrl;
    baseUrl: ResolvedUrl;
    contents: string;
    ast: AstNode;
    isInline: boolean;
    /**
     * If not null, this is an inline document, and astNode is the AST Node of
     * this document inside of the parent. (e.g. the <style> or <script> tag)
     */
    readonly astNode: AstNodeWithLanguage | undefined;
    sourceRange: SourceRange;
    private readonly _locationOffset;
    /**
     * The 0-based offsets into `contents` of all newline characters.
     *
     * Useful for converting between string offsets and SourcePositions.
     */
    readonly newlineIndexes: number[];
    constructor(from: Options<AstNode>);
    /**
     * Runs a set of document-type specific visitors against the document.
     */
    abstract visit(visitors: Visitor[]): void;
    sourceRangeForNode(node: AstNode): SourceRange | undefined;
    protected abstract _sourceRangeForNode(node: AstNode): SourceRange | undefined;
    /**
     * Convert `this.ast` back into a string document.
     */
    abstract stringify(options: StringifyOptions): string;
    offsetToSourcePosition(offset: number): SourcePosition;
    offsetsToSourceRange(start: number, end: number): SourceRange;
    sourcePositionToOffset(position: SourcePosition): number;
    relativeToAbsoluteSourceRange(sourceRange: SourceRange): SourceRange;
    relativeToAbsoluteSourceRange(sourceRange: undefined): undefined;
    relativeToAbsoluteSourceRange(sourceRange: SourceRange | undefined): SourceRange | undefined;
    absoluteToRelativeSourceRange(sourceRange: SourceRange): SourceRange;
    absoluteToRelativeSourceRange(sourceRange: undefined): undefined;
    absoluteToRelativeSourceRange(sourceRange: SourceRange | undefined): SourceRange | undefined;
    sourceRangeToOffsets(range: SourceRange): [number, number];
    toString(): string;
}
export interface Options<A> {
    url: ResolvedUrl;
    baseUrl: ResolvedUrl | undefined;
    contents: string;
    ast: A;
    locationOffset: LocationOffset | undefined;
    astNode: AstNodeWithLanguage | undefined;
    isInline: boolean;
}
export interface StringifyOptions {
    /** The desired level of indentation of to stringify at. */
    indent?: number;
    /**
     * Parsed (and possibly modified) documents that exist inside this document
     * whose stringified contents should be used instead of what is in `ast`.
     */
    inlineDocuments?: ParsedDocument[];
    /**
     * Decide if the document should be formatted for readability, if supported.
     * This option should default to `true` in subclasses.
     */
    prettyPrint?: boolean;
}
/**
 * Used solely for constructing warnings about unparsable or unloadable
 * documents.
 */
export declare class UnparsableParsedDocument extends ParsedDocument {
    type: string;
    constructor(url: ResolvedUrl, contents: string);
    visit(_visitors: {}[]): void;
    protected _sourceRangeForNode(_node: {}): undefined;
    stringify(): string;
}
