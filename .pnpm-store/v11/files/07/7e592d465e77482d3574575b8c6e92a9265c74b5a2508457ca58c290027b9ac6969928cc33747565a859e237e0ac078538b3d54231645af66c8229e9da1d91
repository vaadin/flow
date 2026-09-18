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
import { NodePath } from '@babel/traverse';
import * as babel from '@babel/types';
import { JavaScriptDocument } from '../javascript/javascript-document';
import { Result } from '../model/analysis';
import { ScannedReference, Warning } from '../model/model';
import { Observer, ScannedPolymerElement } from './polymer-element';
export declare type BehaviorReferenceOrWarning = {
    kind: 'warning';
    warning: Warning;
} | {
    kind: 'behaviorReference';
    reference: ScannedReference<'behavior'>;
};
export declare function getBehaviorReference(argNode: babel.Node, document: JavaScriptDocument, path: NodePath): Result<ScannedReference<'behavior'>, Warning>;
export declare type PropertyHandlers = {
    [key: string]: (node: babel.Node) => void;
};
/**
 * Returns an object containing functions that will annotate `declaration` with
 * the polymer-specific meaning of the value nodes for the named properties.
 */
export declare function declarationPropertyHandlers(declaration: ScannedPolymerElement, document: JavaScriptDocument, path: NodePath): PropertyHandlers;
export declare function extractObservers(observersArray: babel.Node, document: JavaScriptDocument): undefined | {
    observers: Observer[];
    warnings: Warning[];
};
