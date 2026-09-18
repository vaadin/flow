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
import * as jsdoc from '../javascript/jsdoc';
import { ScannedEvent } from '../model/model';
import { ScannedPolymerElement } from './polymer-element';
/**
 * Annotates Hydrolysis scanned features, processing any descriptions as
 * JSDoc.
 *
 * You probably want to use a more specialized version of this, such as
 * `annotateElement`.
 *
 * Processed JSDoc values will be made available via the `jsdoc` property on a
 * scanned feature.
 */
export declare function annotate(feature: {
    jsdoc?: jsdoc.Annotation;
    description?: string;
}): void;
/**
 * Annotates @event, @hero, & @demo tags
 */
export declare function annotateElementHeader(scannedElement: ScannedPolymerElement): void;
/**
 * Annotates event documentation
 */
export declare function annotateEvent(annotation: jsdoc.Annotation): ScannedEvent;
