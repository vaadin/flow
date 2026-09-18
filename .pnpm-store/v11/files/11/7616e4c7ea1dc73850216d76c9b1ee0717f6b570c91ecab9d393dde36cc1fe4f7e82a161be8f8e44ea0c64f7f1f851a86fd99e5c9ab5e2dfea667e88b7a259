/**
 * @license
 * Copyright (c) 2017 The Polymer Project Authors. All rights reserved.
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
import { Visitor } from '../javascript/estree-visitor';
import { JavaScriptDocument } from '../javascript/javascript-document';
import { JavaScriptScanner } from '../javascript/javascript-scanner';
import { ScannedPolymerCoreFeature } from './polymer-core-feature';
/**
 * Scans for Polymer 1.x core "features".
 *
 * In the Polymer 1.x core library, the `Polymer.Base` prototype is dynamically
 * augmented with properties via calls to `Polymer.Base._addFeature`. These
 * calls are spread across multiple files and split between the micro, mini,
 * and standard "feature layers". Polymer 2.x does not use this pattern.
 *
 * Example: https://github.com/Polymer/polymer/blob/1.x/src/mini/debouncer.html
 */
export declare class PolymerCoreFeatureScanner implements JavaScriptScanner {
    scan(document: JavaScriptDocument, visit: (visitor: Visitor) => Promise<void>): Promise<{
        features: ScannedPolymerCoreFeature[];
    }>;
}
