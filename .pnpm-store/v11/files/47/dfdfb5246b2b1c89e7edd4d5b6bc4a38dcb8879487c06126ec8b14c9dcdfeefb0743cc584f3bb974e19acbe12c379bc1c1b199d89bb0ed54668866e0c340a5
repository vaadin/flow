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
import { Document } from './document';
import { ElementBase, ElementBaseInit, ScannedElementBase } from './element-base';
import { Feature } from './feature';
import { ScannedReference } from './reference';
export { Visitor } from '../javascript/estree-visitor';
export declare class ScannedElement extends ScannedElementBase {
    tagName?: string;
    className?: string;
    readonly name: string | undefined;
    superClass?: ScannedReference<'class'>;
    /**
     * For customized built-in elements, the tagname of the superClass.
     */
    extends?: string;
    resolve(document: Document): Element;
}
export interface ElementInit extends ElementBaseInit {
    tagName?: string;
    className?: string;
    superClass?: ScannedReference<'class'>;
    extends?: string;
}
declare module './queryable' {
    interface FeatureKindMap {
        'element': Element;
    }
}
export declare class Element extends ElementBase implements Feature {
    readonly tagName: string | undefined;
    /**
     * For customized built-in elements, the tagname of the builtin element that
     * this element extends.
     */
    extends?: string;
    constructor(init: ElementInit, document: Document);
}
