/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { getFlatIndexContext } from './helpers.js';

/**
 * A class that stores items with their associated sub-caches.
 */
export class Cache {
  /**
   * A context object.
   *
   * @type {{ isExpanded: (item: unknown) => boolean }}
   */
  context;

  /**
   * The number of items to display per page.
   *
   * @type {number}
   */
  pageSize;

  /**
   * An array of cached items.
   *
   * @type {object[]}
   */
  items = [];

  /**
   * A map where the key is a requested page and the value is a callback
   * that will be called with data once the request is complete.
   *
   * @type {Record<number, Function>}
   */
  pendingRequests = {};

  /**
   * A map where the key is the index of an item in the `items` array
   * and the value is a sub-cache associated with that item.
   *
   * Note, it's intentionally defined as an object instead of a Map
   * to ensure that Object.entries() returns an array with keys sorted
   * in alphabetical order, rather than the order they were added.
   *
   * @type {Record<number, Cache>}
   * @private
   */
  __subCacheByIndex = {};

  /**
   * The number of items.
   *
   * @type {number}
   * @private
   */
  __size = 0;

  /**
   * The total number of items, including items from expanded sub-caches.
   *
   * @type {number}
   * @private
   */
  __flatSize = 0;

  /**
   * @param {Cache['context']} context
   * @param {number} pageSize
   * @param {number | undefined} size
   * @param {Cache | undefined} parentCache
   * @param {number | undefined} parentCacheIndex
   */
  constructor(context, pageSize, size, parentCache, parentCacheIndex) {
    this.context = context;
    this.pageSize = pageSize;
    this.size = size;
    this.parentCache = parentCache;
    this.parentCacheIndex = parentCacheIndex;
    this.__flatSize = size || 0;
  }

  /**
   * An item in the parent cache that the current cache is associated with.
   *
   * @return {object | undefined}
   */
  get parentItem() {
    return this.parentCache && this.parentCache.items[this.parentCacheIndex];
  }

  /**
   * An array of sub-caches sorted in the same order as their associated items
   * appear in the `items` array.
   *
   * @return {Cache[]}
   */
  get subCaches() {
    return Object.values(this.__subCacheByIndex);
  }

  /**
   * Whether the cache or any of its descendant caches have pending requests.
   *
   * @return {boolean}
   */
  get isLoading() {
    if (Object.keys(this.pendingRequests).length > 0) {
      return true;
    }

    return this.subCaches.some((subCache) => subCache.isLoading);
  }

  /**
   * The total number of items, including items from expanded sub-caches.
   *
   * @return {number}
   */
  get flatSize() {
    return this.__flatSize;
  }

  /**
   * The total number of items, including items from expanded sub-caches.
   *
   * @protected
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  get effectiveSize() {
    console.warn(
      '<vaadin-grid> The `effectiveSize` property of ItemCache is deprecated and will be removed in Vaadin 25.',
    );
    return this.flatSize;
  }

  /**
   * The number of items.
   *
   * @return {number}
   */
  get size() {
    return this.__size;
  }

  /**
   * Sets the number of items.
   *
   * @param {number} size
   */
  set size(size) {
    const oldSize = this.__size;
    if (oldSize === size) {
      return;
    }

    this.__size = size;

    if (this.context.placeholder !== undefined) {
      this.items.length = size || 0;
      for (let i = 0; i < size || 0; i++) {
        this.items[i] ||= this.context.placeholder;
      }
    }

    Object.keys(this.pendingRequests).forEach((page) => {
      const startIndex = parseInt(page) * this.pageSize;
      if (startIndex >= this.size || 0) {
        delete this.pendingRequests[page];
      }
    });
  }

  /**
   * Recalculates the flattened size for the cache and its descendant caches recursively.
   */
  recalculateFlatSize() {
    this.__flatSize =
      !this.parentItem || this.context.isExpanded(this.parentItem)
        ? this.size +
          this.subCaches.reduce((total, subCache) => {
            subCache.recalculateFlatSize();
            return total + subCache.flatSize;
          }, 0)
        : 0;
  }

  /**
   * Adds an array of items corresponding to the given page
   * to the `items` array.
   *
   * @param {number} page
   * @param {object[]} items
   */
  setPage(page, items) {
    const startIndex = page * this.pageSize;
    items.forEach((item, i) => {
      const itemIndex = startIndex + i;
      if (this.size === undefined || itemIndex < this.size) {
        this.items[itemIndex] = item;
      }
    });
  }

  /**
   * Retrieves the sub-cache associated with the item at the given index
   * in the `items` array.
   *
   * @param {number} index
   * @return {Cache | undefined}
   */
  getSubCache(index) {
    return this.__subCacheByIndex[index];
  }

  /**
   * Removes the sub-cache associated with the item at the given index
   * in the `items` array.
   *
   * @param {number} index
   */
  removeSubCache(index) {
    delete this.__subCacheByIndex[index];
  }

  /**
   * Removes all sub-caches.
   */
  removeSubCaches() {
    this.__subCacheByIndex = {};
  }

  /**
   * Creates and associates a sub-cache for the item at the given index
   * in the `items` array.
   *
   * @param {number} index
   * @return {Cache}
   */
  createSubCache(index) {
    const subCache = new Cache(this.context, this.pageSize, 0, this, index);
    this.__subCacheByIndex[index] = subCache;
    return subCache;
  }

  /**
   * Retrieves the flattened index corresponding to the given index
   * of an item in the `items` array.
   *
   * @param {number} index
   * @return {number}
   */
  getFlatIndex(index) {
    const clampedIndex = Math.max(0, Math.min(this.size - 1, index));

    return this.subCaches.reduce((prev, subCache) => {
      const index = subCache.parentCacheIndex;
      return clampedIndex > index ? prev + subCache.flatSize : prev;
    }, clampedIndex);
  }

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  getItemForIndex(index) {
    console.warn(
      '<vaadin-grid> The `getItemForIndex` method of ItemCache is deprecated and will be removed in Vaadin 25.',
    );
    const { item } = getFlatIndexContext(this, index);
    return item;
  }

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  getCacheAndIndex(index) {
    console.warn(
      '<vaadin-grid> The `getCacheAndIndex` method of ItemCache is deprecated and will be removed in Vaadin 25.',
    );
    const { cache, index: scaledIndex } = getFlatIndexContext(this, index);
    return { cache, scaledIndex };
  }

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  updateSize() {
    console.warn('<vaadin-grid> The `updateSize` method of ItemCache is deprecated and will be removed in Vaadin 25.');
    this.recalculateFlatSize();
  }

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  ensureSubCacheForScaledIndex(scaledIndex) {
    console.warn(
      '<vaadin-grid> The `ensureSubCacheForScaledIndex` method of ItemCache is deprecated and will be removed in Vaadin 25.',
    );

    if (!this.getSubCache(scaledIndex)) {
      const subCache = this.createSubCache(scaledIndex);
      this.context.__controller.__loadCachePage(subCache, 0);
    }
  }

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  get grid() {
    console.warn('<vaadin-grid> The `grid` property of ItemCache is deprecated and will be removed in Vaadin 25.');
    return this.context.__controller.host;
  }

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  get itemCaches() {
    console.warn(
      '<vaadin-grid> The `itemCaches` property of ItemCache is deprecated and will be removed in Vaadin 25.',
    );
    return this.__subCacheByIndex;
  }
}
