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
/**
 * The analyzer only uses one method from CancelTokens, and it's likely to be
 * the least contentious, so even if other parts change on the way to
 * standardization,
 * this probably won't.
 */
export interface MinimalCancelToken {
    /**
     * If the token has been cancelled then this method throws a Cancel, otherwise
     * it returns undefined.
     *
     * A Cancel is an object constructed by a constructor named 'Cancel'.
     */
    throwIfRequested(): void;
}
export declare const neverCancels: MinimalCancelToken;
