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
import { Document, Feature, Method, Property, Resolvable, ScannedFeature, ScannedMethod, ScannedProperty, Warning } from '../model/model';
/**
 * A scanned Polymer 1.x core "feature".
 */
export declare class ScannedPolymerCoreFeature extends ScannedFeature implements Resolvable {
    warnings: Warning[];
    properties: Map<string, ScannedProperty>;
    methods: Map<string, ScannedMethod>;
    resolve(document: Document): Feature | undefined;
}
declare module '../model/queryable' {
    interface FeatureKindMap {
        'polymer-core-feature': PolymerCoreFeature;
    }
}
/**
 * A resolved Polymer 1.x core "feature".
 */
export declare class PolymerCoreFeature implements Feature {
    properties: Map<string, Property>;
    methods: Map<string, Method>;
    kinds: Set<string>;
    identifiers: Set<string>;
    warnings: Warning[];
    readonly statementAst: undefined;
    constructor(properties: Map<string, Property>, methods: Map<string, Method>);
}
