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
import { SourceRange } from '../analysis-format/analysis-format';
import { AnalysisContext } from '../core/analysis-context';
import { Document } from './document';
import { Feature } from './feature';
import { AnalysisQuery as Query, AnalysisQueryWithKind as QueryWithKind, FeatureKind, FeatureKindMap, Queryable } from './queryable';
import { ResolvedUrl } from './url';
import { Warning } from './warning';
/**
 * Represents the result of a computation that may fail.
 *
 * This lets us represent errors in a type-safe way, as well as
 * in a way that makes it clear to the caller that the computation
 * may fail.
 */
export declare type Result<T, E> = {
    successful: true;
    value: T;
} | {
    successful: false;
    error: E;
};
/**
 * Represents a queryable interface over all documents in a package/project.
 *
 * Results of queries will include results from all documents in the package, as
 * well as from external dependencies that are transitively imported by
 * documents in the package.
 */
export declare class Analysis implements Queryable {
    private readonly context;
    private readonly _results;
    private readonly _searchRoots;
    static isExternal(path: string): boolean;
    constructor(results: Map<ResolvedUrl, Document | Warning>, context: AnalysisContext);
    getDocument(packageRelativeUrl: string): Result<Document, Warning | undefined>;
    /**
     * Get features in the package.
     *
     * Be default this includes features in all documents inside the package,
     * but you can specify whether to also include features that are outside the
     * package reachable by documents inside. See the documentation for Query for
     * more details.
     *
     * You can also narrow by feature kind and identifier.
     */
    getFeatures<K extends FeatureKind>(query: QueryWithKind<K>): Set<FeatureKindMap[K]>;
    getFeatures(query?: Query): Set<Feature>;
    /**
     * Get all warnings in the project.
     */
    getWarnings(options?: Query): Warning[];
    /**
     * Potentially narrow down the document that contains the sourceRange.
     * For example, if a source range is inside a inlineDocument, this function
     * will narrow down the document to the most specific inline document.
     *
     * @param sourceRange Source range to search for in a document
     */
    getDocumentContaining(sourceRange: SourceRange | undefined): Document | undefined;
    private _getDocumentQuery;
}
