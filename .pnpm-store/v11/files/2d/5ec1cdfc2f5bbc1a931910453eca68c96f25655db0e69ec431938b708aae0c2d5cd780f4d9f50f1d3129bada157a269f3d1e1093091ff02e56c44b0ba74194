/**
 * @license
 * Copyright (c) 2026 - 2026 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * Recursively copies own properties of `source` into `target` and returns
 * `target`. Plain objects are merged, other values are assigned as they are.
 * An object is plain when it inherits from `Object.prototype` or from nothing,
 * so values such as a `Date` or a class instance are assigned, not merged.
 *
 * Merges a single source. Use `deepMergePartials()` to merge several objects,
 * or to merge objects that only provide some of the properties.
 *
 * Both arguments are expected to be plain objects. When either of them is not,
 * `target` is returned without changes. A property of the target that is not a
 * plain object is replaced with the merged object, unlike the arguments.
 *
 * Keys that would modify `Object.prototype`, such as `__proto__`, are ignored.
 */
export function deepMerge<T extends object>(target: T, source: object): T;

/**
 * Recursively merges partial objects into `target` in order and returns
 * `target`, so that a later source overrides an earlier one.
 *
 * Values that are `null` or `undefined` are skipped, so a source that only
 * provides some of the properties does not remove the others. For the same
 * reason, a property that the target already has is not replaced with an
 * object when the source has one for the same key. Arrays are copied one level
 * deep, so that the result does not share an array with any of the sources.
 *
 * Sources that are not plain objects are ignored. When `target` is not a plain
 * object, it is returned without changes.
 *
 * Keys that would modify `Object.prototype`, such as `__proto__`, are ignored.
 */
export function deepMergePartials<T extends object>(target: T, ...sources: object[]): T;
