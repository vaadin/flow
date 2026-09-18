/**
 * A WeakMap that maps keys to Sets of values, allowing multiple values per key.
 */
export default class MultiWeakMap extends WeakMap<object, any> {
    constructor(entries?: readonly (readonly [object, any])[]);
    constructor(iterable: Iterable<readonly [any, any]>);
    has(key: any, value: any, ...args: any[]): any;
    add(key: any, value: any): void;
    delete(key: any, ...values: any[]): void;
}
