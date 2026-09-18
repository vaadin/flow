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
import { Feature } from './feature';
import { Warning } from './warning';
export interface FeatureKindMap {
}
export declare type FeatureKind = keyof FeatureKindMap;
export declare type BaseQueryOptions = {
    /**
     * Do not include the containing document or any features from the
     * containing document; allows querying for features specific to an inline
     * document and/or its descendents.
     */
    excludeBackreferences?: boolean;
    /**
     * If true then results will include features from outside the package, e.g.
     * from files in bower_components or node_modules directories.
     *
     * Note that even with this option you'll only get results from external files
     * that are referenced from within the package.
     */
    externalPackages?: boolean;
    /**
     * Do not include any features that are only reachable via paths that include
     * lazy import edges.
     */
    noLazyImports?: boolean;
    /**
     * If given, the query results will all have the given identifier.
     *
     * There identifiers mean different things for different kinds of features.
     * For example documents are identified by their url, and elements are
     * identified by their tag and class names.
     */
    id?: string;
} & object;
export declare type BaseAnalysisQuery = BaseQueryOptions & {
    /**
     * When querying over an Analysis, the results would not be defined if
     * imports are not considered, so it is legal to specify this parameter,
     * but it must be true (and it will be ignored in any case).
     */
    imported?: true;
};
export declare type BaseDocumentQuery = BaseQueryOptions & {
    /**
     * If given, returns any features which are defined by the given statement.
     * This argument implies `imported: false`.
     *
     * See esutil.getStatement for going from a babel-traverse NodePath to the
     * canonical statment node.
     */
    statement?: babel.Statement;
    /**
     * If true, the query will return results from the document and its
     * dependencies. Otherwise it will only include results from the document.
     */
    imported?: boolean;
};
export declare type BaseQuery = BaseQueryOptions & {
    kind?: string;
};
export declare type BaseQueryWithKind<K extends FeatureKind> = BaseQueryOptions & {
    kind: K;
};
export declare type DocumentQuery = BaseDocumentQuery & {
    kind?: string;
};
export declare type DocumentQueryWithKind<K extends FeatureKind> = BaseDocumentQuery & {
    kind: K;
};
export declare type AnalysisQuery = BaseAnalysisQuery & {
    kind?: string;
};
export declare type AnalysisQueryWithKind<K extends FeatureKind> = BaseAnalysisQuery & {
    kind: K;
};
/**
 * Represents something like a Document or an Analysis. A container of features
 * and warnings that's queryable in a few different ways.
 */
export interface Queryable {
    getFeatures<K extends FeatureKind>(query: BaseQueryWithKind<K>): Set<FeatureKindMap[K]>;
    getFeatures(query?: BaseQuery): Set<Feature>;
    getWarnings(options?: BaseQuery): Warning[];
}
