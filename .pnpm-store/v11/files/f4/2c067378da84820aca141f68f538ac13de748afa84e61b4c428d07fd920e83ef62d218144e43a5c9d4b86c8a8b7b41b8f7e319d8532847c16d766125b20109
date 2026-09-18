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
export declare type CancelFunction = (reason?: any) => void;
/**
 * A polyfill for the November 23rd, 2016 draft of
 * https://tc39.github.io/proposal-cancelable-promises/
 */
export declare class CancelToken {
    promise: Promise<Cancel>;
    reason: Cancel | undefined;
    constructor(executor: (cancel: CancelFunction) => void);
    static source(): {
        token: CancelToken;
        cancel: CancelFunction;
    };
    static race(tokens: Iterable<CancelToken>): CancelToken;
    throwIfRequested(): void;
    readonly [Symbol.toStringTag]: string;
}
export declare class Cancel {
    message: string;
    constructor(reason?: any);
    toString(): string;
}
export declare function isCancel(value: any): value is Cancel;
export declare function isCancelToken(value: any): value is CancelToken;
