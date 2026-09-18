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
import { Document } from '../model/model';
import { Options as ElementOptions, PolymerElement, ScannedPolymerElement } from '../polymer/polymer-element';
export interface Options extends ElementOptions {
}
/**
 * The metadata for a Polymer behavior mixin.
 */
export declare class ScannedBehavior extends ScannedPolymerElement {
    tagName: undefined;
    className?: string;
    resolve(document: Document): Behavior;
}
declare module '../model/queryable' {
    interface FeatureKindMap {
        'behavior': Behavior;
    }
}
export declare class Behavior extends PolymerElement {
    readonly tagName: undefined;
    constructor(scannedBehavior: ScannedBehavior, document: Document);
    toString(): string;
}
