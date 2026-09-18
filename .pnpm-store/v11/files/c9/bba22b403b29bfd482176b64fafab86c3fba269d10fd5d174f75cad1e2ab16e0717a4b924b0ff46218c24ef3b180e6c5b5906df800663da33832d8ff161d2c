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
import { JsTransformOptions } from './js-transform';
/**
 * Options for htmlTransform.
 */
export interface HtmlTransformOptions {
    /**
     * Transformations to apply to JavaScript within the HTML document.
     */
    js?: JsTransformOptions;
    /**
     * Whether to minify HTML.
     */
    minifyHtml?: boolean;
    /**
     * Whether and which Babel helpers to inject as an inline script. This is
     * typically needed when this is the entry point HTML document and ES5
     * compilation or AMD transform is enabled.
     *
     * If "none" (the default), no helpers are injected. If "full", includes the
     * helpers needed for both ES5 compilation and the AMD transform. If "amd",
     * includes only the helpers needed for the AMD transform.
     */
    injectBabelHelpers?: 'none' | 'full' | 'amd';
    /**
     * Whether to inject the regenerator runtime as an inline script. This is
     * needed if you are compiling to ES5 and use async/await or generators.
     */
    injectRegeneratorRuntime?: boolean;
    /**
     * Whether to inject an AMD loader as an inline script. This is typically
     * needed if ES to AMD module transformation is enabled and this is the entry
     * point HTML document.
     */
    injectAmdLoader?: boolean;
}
/**
 * Transform some HTML according to the given options.
 */
export declare function htmlTransform(html: string, options: HtmlTransformOptions): string;
