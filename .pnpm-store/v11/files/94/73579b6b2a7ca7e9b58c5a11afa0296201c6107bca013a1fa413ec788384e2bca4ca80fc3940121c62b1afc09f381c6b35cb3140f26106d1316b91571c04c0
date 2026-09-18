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
import { RequestHandler } from 'express';
/**
 * Returns an express middleware that injects the Custom Elements ES5 Adapter
 * into the entry point when we are serving ES5.
 *
 * This is a *transforming* middleware, so it must be installed before the
 * middleware that actually serves the entry point.
 */
export declare function injectCustomElementsEs5Adapter(compile?: 'always' | 'never' | 'auto'): RequestHandler;
