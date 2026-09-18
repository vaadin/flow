/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
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
import { JsCompileTarget, ModuleResolutionStrategy } from 'polymer-project-config';
/**
 * Options for jsTransform.
 */
export interface JsTransformOptions {
    compile?: boolean | JsCompileTarget;
    externalHelpers?: boolean;
    minify?: boolean;
    moduleResolution?: ModuleResolutionStrategy;
    filePath?: string;
    packageName?: string;
    isComponentRequest?: boolean;
    componentDir?: string;
    rootDir?: string;
    transformModulesToAmd?: boolean | 'auto';
    softSyntaxError?: boolean;
}
/**
 * Transform some JavaScript according to the given options.
 */
export declare function jsTransform(js: string, options: JsTransformOptions): string;
