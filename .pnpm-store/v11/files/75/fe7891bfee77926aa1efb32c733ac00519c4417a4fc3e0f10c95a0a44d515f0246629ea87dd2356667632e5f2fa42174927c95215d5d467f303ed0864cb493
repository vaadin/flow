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
import { Property, ScannedProperty } from './model';
export interface ScannedMethod extends ScannedProperty {
    params?: MethodParam[];
    return?: {
        type?: string;
        desc?: string;
    };
}
export interface Method extends Property {
    readonly params?: MethodParam[];
    readonly return?: {
        type?: string;
        desc?: string;
    };
}
export interface MethodParam {
    readonly name: string;
    readonly type?: string;
    readonly defaultValue?: string;
    readonly rest?: boolean;
    readonly description?: string;
}
