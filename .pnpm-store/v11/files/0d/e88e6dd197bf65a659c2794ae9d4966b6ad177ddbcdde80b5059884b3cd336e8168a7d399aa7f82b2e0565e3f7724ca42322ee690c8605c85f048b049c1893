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
import { JsAstNode } from '..';
import { ScannedFeature } from './feature';
export interface ScannedEvent extends ScannedFeature {
    readonly name: string;
    readonly type?: string;
    readonly description?: string;
    readonly params: {
        type: string;
        desc: string;
        name: string;
    }[];
    readonly astNode: JsAstNode | undefined;
}
export interface Event {
    readonly name: string;
    readonly type?: string;
    readonly description?: string;
    readonly astNode: JsAstNode | undefined;
    readonly inheritedFrom?: string;
}
