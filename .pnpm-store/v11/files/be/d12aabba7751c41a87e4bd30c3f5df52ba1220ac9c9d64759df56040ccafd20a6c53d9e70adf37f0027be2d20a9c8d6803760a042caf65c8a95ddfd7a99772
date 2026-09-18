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
import { Analyzer } from '../core/analyzer';
import { UrlResolver } from '../index';
import { ParsedDocument } from '../parser/document';
import { Analysis } from './analysis';
import { SourceRange } from './source-range';
import { ResolvedUrl } from './url';
export interface WarningInit {
    readonly message: string;
    readonly sourceRange: SourceRange;
    readonly severity: Severity;
    readonly code: string;
    readonly parsedDocument: ParsedDocument;
    readonly fix?: Edit;
    readonly actions?: ReadonlyArray<Action>;
}
export declare class Warning {
    readonly code: string;
    readonly message: string;
    readonly sourceRange: SourceRange;
    readonly severity: Severity;
    /**
     * If the problem has a single automatic fix, this is it.
     *
     * Whether and how much something is 'automatic' can be a bit tricky to
     * delineate. Roughly speaking, if 99% of the time the change solves the
     * issue completely then it should go in `fix`.
     */
    readonly fix: Edit | undefined;
    /**
     * Other actions that could be taken in response to this warning.
     *
     * Each action is separate and they may be mutually exclusive. In the case
     * of edit actions they often are.
     */
    readonly actions: ReadonlyArray<Action> | undefined;
    private readonly _parsedDocument;
    constructor(init: WarningInit);
    toString(options?: Partial<WarningStringifyOptions>): string;
    private _severityToColorFunction;
    private _severityToString;
    toJSON(): {
        code: string;
        message: string;
        severity: Severity;
        sourceRange: SourceRange;
    };
}
export declare enum Severity {
    ERROR = 0,
    WARNING = 1,
    INFO = 2
}
export declare class WarningCarryingException extends Error {
    readonly warning: Warning;
    constructor(warning: Warning);
}
export declare type Verbosity = 'one-line' | 'full' | 'code-only';
export interface WarningStringifyOptions {
    readonly verbosity: Verbosity;
    readonly color: boolean;
    /**
     * If given, we will use resolver.relative to get a relative path
     * to the reported file.
     */
    readonly resolver?: UrlResolver;
    /**
     * If given, we will only print this many lines of code. Otherwise print all
     * lines in the source range.
     */
    readonly maxCodeLines?: number;
}
export declare type Action = EditAction | {
    /** To ensure that type safe code actually checks for the action kind. */
    kind: 'never';
};
/**
 * An EditAction is like a fix, only it's not applied automatically when the
 * user runs `polymer lint --fix`. Often this is because it's less safe to
 * apply automatically, and there may be caveats, or multiple ways to resolve
 * the warning.
 *
 * For example, a change to an element that updates it to no longer use a
 * deprecated feature, but that involves a change in the element's API should
 * not be a fix, but should instead be an EditAction.
 */
export interface EditAction {
    kind: 'edit';
    /**
     * A unique string code for the edit action. Useful so that the user can
     * request that all actions with a given code should be applied.
     */
    code: string;
    /**
     * A short description of the change, noting caveats and important information
     * for the user.
     */
    description: string;
    edit: Edit;
}
/**
 * Represents an action for replacing a range in a document with some text.
 *
 * This is sufficient to represent all operations on text files, including
 * inserting and deleting text (using empty ranges or empty replacement
 * text, respectively).
 */
export interface Replacement {
    readonly range: SourceRange;
    readonly replacementText: string;
}
/**
 * A set of replacements that must all be applied as a single atomic unit.
 */
export declare type Edit = ReadonlyArray<Replacement>;
export interface EditResult {
    /** The edits that had no conflicts, and are reflected in editedFiles. */
    appliedEdits: Edit[];
    /** Edits that could not be applied due to overlapping ranges. */
    incompatibleEdits: Edit[];
    /** A map from urls to their new contents. */
    editedFiles: Map<ResolvedUrl, string>;
}
/**
 * Takes the given edits and, provided there are no overlaps, applies them to
 * the contents loadable from the given loader.
 *
 * If there are overlapping edits, then edits earlier in the array get priority
 * over later ones.
 */
export declare function applyEdits(edits: Edit[], loader: (url: ResolvedUrl) => Promise<ParsedDocument>): Promise<EditResult>;
export declare function makeParseLoader(analyzer: Analyzer, analysis?: Analysis): (url: ResolvedUrl) => Promise<ParsedDocument<{} | null | undefined, {}>>;
