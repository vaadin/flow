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
import * as babel from '@babel/types';
import { AnalysisContext } from '../core/analysis-context';
import { ParsedCssDocument } from '../css/css-document';
import { ParsedHtmlDocument } from '../html/html-document';
import { JavaScriptDocument } from '../javascript/javascript-document';
import { ParsedJsonDocument } from '../json/json-document';
import { ParsedDocument } from '../parser/document';
import { ParsedTypeScriptDocument } from '../typescript/typescript-document';
import { Feature, ScannedFeature } from './feature';
import { ImmutableSet } from './immutable';
import { AstNodeWithLanguage } from './inline-document';
import { DocumentQuery as Query, DocumentQueryWithKind as QueryWithKind, FeatureKind, FeatureKindMap, Queryable } from './queryable';
import { SourceRange } from './source-range';
import { ResolvedUrl } from './url';
import { Warning } from './warning';
/**
 * The metadata for all features and elements defined in one document
 */
export declare class ScannedDocument {
    document: ParsedDocument;
    features: ScannedFeature[];
    warnings: Warning[];
    isInline: boolean;
    readonly sourceRange: SourceRange;
    readonly astNode: AstNodeWithLanguage | undefined;
    constructor(document: ParsedDocument, features: ScannedFeature[], warnings?: Warning[]);
    readonly url: ResolvedUrl;
    /**
     * Gets all features in this scanned document and all inline documents it
     * contains.
     */
    getNestedFeatures(): ScannedFeature[];
    private _getNestedFeatures;
}
export interface DeclaredWithStatement {
    /**
     * This is the nearest statement at or above `this.astNode`.
     *
     * This is a fast and simple way to get a canonical, indexable AST node,
     * so that we can look up a feature syntactically.
     *
     * Populate this field with `esutil.getCanonicalStatement`.
     */
    statementAst: babel.Statement | undefined;
}
declare module './queryable' {
    interface FeatureKindMap {
        'document': Document;
        'html-document': Document<ParsedHtmlDocument>;
        'js-document': Document<JavaScriptDocument>;
        'json-document': Document<ParsedJsonDocument>;
        'css-document': Document<ParsedCssDocument>;
        'typescript-document': Document<ParsedTypeScriptDocument>;
    }
}
export declare class Document<ParsedType extends ParsedDocument = ParsedDocument> implements Feature, Queryable {
    readonly kinds: ImmutableSet<string>;
    readonly identifiers: ImmutableSet<string>;
    /**
     * AnalysisContext is a private type. Only internal analyzer code should touch
     * this field.
     */
    _analysisContext: AnalysisContext;
    warnings: Warning[];
    languageAnalysis?: {};
    private readonly _localFeatures;
    private readonly _scannedDocument;
    private readonly _localFeaturesByStatement;
    /**
     * To handle recursive dependency graphs we must track whether we've started
     * resolving this Document so that we can reliably early exit even if one
     * of our dependencies tries to resolve this document.
     */
    private _begunResolving;
    /**
     * True after this document and all of its children are finished resolving.
     */
    private _doneResolving;
    constructor(base: ScannedDocument, analyzer: AnalysisContext, languageAnalysis?: {});
    readonly url: ResolvedUrl;
    readonly isInline: boolean;
    readonly sourceRange: SourceRange | undefined;
    readonly astNode: AstNodeWithLanguage | undefined;
    readonly parsedDocument: ParsedType;
    readonly resolved: boolean;
    readonly type: string;
    /**
     * Resolves all features of this document, so that they have references to all
     * their dependencies.
     *
     * This method can only be called once
     */
    resolve(): void;
    /**
     * Adds and indexes a feature to this document before resolve().
     */
    _addFeature(feature: Feature): void;
    /**
     * Get features on the document.
     *
     * Be default it includes only features on the document, but you can specify
     * whether to include features that are reachable by imports, features from
     * outside the current package, etc. See the documentation for Query for more
     * details.
     *
     * You can also narrow by feature kind and identifier.
     */
    getFeatures<K extends FeatureKind>(query: QueryWithKind<K>): Set<FeatureKindMap[K]>;
    getFeatures(query?: Query): Set<Feature>;
    private _getByKind;
    private _getById;
    private _getByStatement;
    /** Filters out the given features by the given kind. */
    private _filterByKind;
    private _isCachable;
    private _getSlowlyByKind;
    /**
     * Walks the graph of documents, starting from `this`, finding features which
     * match the given query and adding them to the `result` set. Uses `visited`
     * to deal with cycles.
     *
     * This method is O(numFeatures), though it does not walk documents that are
     * not relevant to the query (e.g. based on whether the query follows imports,
     * or excludes lazy imports)
     */
    private _listFeatures;
    private _filterOutExternal;
    /**
     * Get warnings for the document and all matched features.
     */
    getWarnings(query?: Query): Warning[];
    toString(): string;
    private _toString;
    stringify(): string;
    private _indexFeature;
    private _featuresByKind;
    private _featuresByKindAndId;
    private _buildIndexes;
}
