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
import { FileRelativeUrl } from '../model/model';
/**
 * Resolves module specifiers using node module resolution.
 *
 * Full URLs - those parsable by the WHATWG URL spec - are returned as-is.
 * Absolute and relative paths are resolved, even though they are valid
 * HTML-spec module specifiers, because node resolution supports directories
 * and omitting extensions. If a specifier doesn't resolve, it's returned as-is.
 *
 * @param componentInfo An object describing a "component-style" URL layout. In
 *   this layout, cross-package URLs reach out of the package directory to
 *   sibling packages, rather than into the component directory. When given,
 *   this parameter causes relative paths to be returns for this style.
 */
export declare const resolve: (specifier: string, documentPath: string, componentInfo?: {
    packageName: string;
    rootDir: string;
    componentDir: string;
} | undefined) => FileRelativeUrl;
