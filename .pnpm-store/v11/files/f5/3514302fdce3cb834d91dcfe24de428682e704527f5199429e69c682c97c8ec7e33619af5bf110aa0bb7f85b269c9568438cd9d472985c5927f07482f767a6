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
/// <reference types="node" />
import { JsCompileTarget, ModuleResolutionStrategy } from 'polymer-project-config';
import { Transform } from 'stream';
import * as vinyl from 'vinyl';
import File = require('vinyl');
export declare type FileCB = (error?: Error, file?: File) => void;
export declare type CSSOptimizeOptions = {
    stripWhitespace?: boolean;
};
export interface OptimizeOptions {
    html?: {
        minify?: boolean | {
            exclude?: string[];
        };
    };
    css?: {
        minify?: boolean | {
            exclude?: string[];
        };
    };
    js?: JsOptimizeOptions;
    entrypointPath?: string;
    rootDir?: string;
}
export declare type JsCompileOptions = boolean | JsCompileTarget | {
    target?: JsCompileTarget;
    exclude?: string[];
};
export interface JsOptimizeOptions {
    minify?: boolean | {
        exclude?: string[];
    };
    compile?: JsCompileOptions;
    moduleResolution?: ModuleResolutionStrategy;
    transformModulesToAmd?: boolean;
}
/**
 * GenericOptimizeTransform is a generic optimization stream. It can be extended
 * to create a new kind of specific file-type optimizer, or it can be used
 * directly to create an ad-hoc optimization stream for different libraries.
 * If the transform library throws an exception when run, the file will pass
 * through unaffected.
 */
export declare class GenericOptimizeTransform extends Transform {
    optimizer: (content: string, file: File) => string;
    optimizerName: string;
    constructor(optimizerName: string, optimizer: (content: string, file: File) => string);
    _transform(file: File, _encoding: string, callback: FileCB): void;
}
/**
 * Transform JavaScript.
 */
export declare class JsTransform extends GenericOptimizeTransform {
    constructor(options: OptimizeOptions);
}
/**
 * Transform HTML.
 */
export declare class HtmlTransform extends GenericOptimizeTransform {
    constructor(options: OptimizeOptions);
}
/**
 * CSSMinifyTransform minifies CSS that pass through it (via css-slam).
 */
export declare class CSSMinifyTransform extends GenericOptimizeTransform {
    private options;
    constructor(options: CSSOptimizeOptions);
    _transform(file: File, encoding: string, callback: FileCB): void;
}
/**
 * InlineCSSOptimizeTransform minifies inlined CSS (found in HTML files) that
 * passes through it (via css-slam).
 */
export declare class InlineCSSOptimizeTransform extends GenericOptimizeTransform {
    private options;
    constructor(options: CSSOptimizeOptions);
    _transform(file: File, encoding: string, callback: FileCB): void;
}
/**
 * Returns an array of optimization streams to use in your build, based on the
 * OptimizeOptions given.
 */
export declare function getOptimizeStreams(options?: OptimizeOptions): NodeJS.ReadWriteStream[];
export declare function matchesExt(extension: string): (fs: vinyl) => boolean;
