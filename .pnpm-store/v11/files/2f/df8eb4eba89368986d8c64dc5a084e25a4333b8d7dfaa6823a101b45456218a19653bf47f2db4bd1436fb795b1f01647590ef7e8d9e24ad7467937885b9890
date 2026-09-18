/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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
import { ResolvedUrl } from '../model/url';
/**
 * Maintains bidirectional indexes of the dependency graph, for quick querying.
 */
export declare class DependencyGraph {
    private _documents;
    constructor(from?: DependencyGraph);
    private _getRecordFor;
    /**
     * Add dependencies of the given path.
     *
     * @param url The url of a document.
     * @param newDependencies The paths of that document's direct dependencies.
     */
    addDocument(url: ResolvedUrl, dependencies: Iterable<ResolvedUrl>): void;
    rejectDocument(url: ResolvedUrl, error: Error): void;
    /**
     * Returns a Promise that resolves when the given document and all
     * of its transitive dependencies have been resolved or rejected. This
     * Promise never rejects, if the document or any dependencies are rejected,
     * the Promise still resolves.
     */
    whenReady(url: ResolvedUrl): Promise<void>;
    private _whenReady;
    /**
     * Returns a fork of this graph without the documents at the given paths.
     */
    invalidatePaths(paths: ResolvedUrl[]): DependencyGraph;
    /**
     * Returns the set of transitive dependencies on the given path.
     *
     * So if A depends on B which depends on C, then getAllDependentsOf(C) will
     * be Set([A,B]), and getAllDependantsOf(B) will be Set([A]).
     */
    getAllDependantsOf(path: ResolvedUrl): Set<ResolvedUrl>;
    private _getAllDependantsOf;
    toString(): string;
}
