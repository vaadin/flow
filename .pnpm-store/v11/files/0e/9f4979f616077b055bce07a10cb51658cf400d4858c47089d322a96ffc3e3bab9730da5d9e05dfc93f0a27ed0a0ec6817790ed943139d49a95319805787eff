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
import { MinimalCancelToken } from './cancel-token';
/**
 * A map from keys to promises of values. Used for caching asynchronous work.
 */
export declare class AsyncWorkCache<K, V> {
    private _keyToResultMap;
    constructor(from?: AsyncWorkCache<K, V>);
    /**
     * If work has already begun to compute the given key, return a promise for
     * the result of that work.
     *
     * If not, compute it with the given function.
     *
     * This method ensures that, in the absence of cancellations, we will only try
     * to compute the value for `key` once, no matter how often or with what
     * timing getOrCompute is called, even recursively.
     *
     * This API is safe for multiple, independently cancellable callers. So long
     * as the given cancelToken is not cancelled, this function will not reject
     * with a Cancel exception.
     */
    getOrCompute(key: K, compute: () => Promise<V>, cancelToken?: MinimalCancelToken): Promise<V>;
    private _getOrCompute;
    delete(key: K): void;
    clear(): void;
    set(key: K, value: V): void;
    has(key: K): boolean;
}
