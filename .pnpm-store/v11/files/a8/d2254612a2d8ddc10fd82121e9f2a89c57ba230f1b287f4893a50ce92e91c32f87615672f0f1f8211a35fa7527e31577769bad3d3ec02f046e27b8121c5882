/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import { Cache } from './cache.js';
import { getFlatIndexByPath, getFlatIndexContext, getItemContext } from './helpers.js';

/**
 * A controller that stores and manages items loaded with a data provider.
 */
export class DataProviderController extends EventTarget {
  /**
   * The controller host element.
   *
   * @param {HTMLElement}
   */
  host;

  /**
   * A callback that returns data based on the passed params such as
   * `page`, `pageSize`, `parentItem`, etc.
   */
  dataProvider;

  /**
   * A callback that returns additional params that need to be passed
   * to the data provider callback with every request.
   */
  dataProviderParams;

  /**
   * A number of items to display per page.
   *
   * @type {number}
   */
  pageSize;

  /**
   * A callback that returns whether the given item is expanded.
   *
   * @type {(item: unknown) => boolean}
   */
  isExpanded;

  /**
   * A callback that returns the id for the given item and that
   * is used when checking object items for equality.
   *
   * @type { (item: unknown) => unknown}
   */
  getItemId;

  /**
   * A reference to the root cache instance.
   *
   * @param {Cache}
   */
  rootCache;

  /**
   * A placeholder item that is used to indicate that the item is not loaded yet.
   *
   * @type {unknown}
   */
  placeholder;

  /**
   * A callback that returns whether the given item is a placeholder.
   *
   * @type {(item: unknown) => boolean}
   */
  isPlaceholder;

  constructor(
    host,
    { size, pageSize, isExpanded, getItemId, isPlaceholder, placeholder, dataProvider, dataProviderParams },
  ) {
    super();
    this.host = host;
    this.pageSize = pageSize;
    this.getItemId = getItemId;
    this.isExpanded = isExpanded;
    this.placeholder = placeholder;
    this.isPlaceholder = isPlaceholder;
    this.dataProvider = dataProvider;
    this.dataProviderParams = dataProviderParams;
    this.rootCache = this.__createRootCache(size);
  }

  /**
   * The total number of items, including items from expanded sub-caches.
   */
  get flatSize() {
    return this.rootCache.flatSize;
  }

  /** @private */
  get __cacheContext() {
    return {
      isExpanded: this.isExpanded,
      placeholder: this.placeholder,
      // The controller instance is needed to ensure deprecated cache methods work.
      __controller: this,
    };
  }

  /**
   * Whether the root cache or any of its decendant caches have pending requests.
   *
   * @return {boolean}
   */
  isLoading() {
    return this.rootCache.isLoading;
  }

  /**
   * Sets the page size and clears the cache.
   *
   * @param {number} pageSize
   */
  setPageSize(pageSize) {
    this.pageSize = pageSize;
    this.clearCache();
  }

  /**
   * Sets the data provider callback and clears the cache.
   *
   * @type {Function}
   */
  setDataProvider(dataProvider) {
    this.dataProvider = dataProvider;
    this.clearCache();
  }

  /**
   * Recalculates the flattened size.
   */
  recalculateFlatSize() {
    this.rootCache.recalculateFlatSize();
  }

  /**
   * Clears the cache.
   */
  clearCache() {
    this.rootCache = this.__createRootCache(this.rootCache.size);
  }

  /**
   * Returns context for the given flattened index, including:
   * - the corresponding cache
   * - the cache level
   * - the corresponding item (if loaded)
   * - the item's index in the cache's items array
   * - the page containing the item
   *
   * @param {number} flatIndex
   */
  getFlatIndexContext(flatIndex) {
    return getFlatIndexContext(this.rootCache, flatIndex);
  }

  /**
   * Returns context for the given item, including:
   * - the cache containing the item
   * - the cache level
   * - the item
   * - the item's index in the cache's items array
   * - the item's flattened index
   * - the item's sub-cache (if exists)
   * - the page containing the item
   *
   * If the item isn't found, the method returns undefined.
   */
  getItemContext(item) {
    return getItemContext({ getItemId: this.getItemId }, this.rootCache, item);
  }

  /**
   * Returns the flattened index for the item that the given indexes point to.
   * Each index in the path array points to a sub-item of the previous index.
   * Using `Infinity` as an index will point to the last item on the level.
   *
   * @param {number[]} path
   * @return {number}
   */
  getFlatIndexByPath(path) {
    return getFlatIndexByPath(this.rootCache, path);
  }

  /**
   * Requests the data provider to load the page with the item corresponding
   * to the given flattened index. If the item is already loaded, the method
   * returns immediatelly.
   *
   * @param {number} flatIndex
   */
  ensureFlatIndexLoaded(flatIndex) {
    const { cache, page, item } = this.getFlatIndexContext(flatIndex);

    if (!this.__isItemLoaded(item)) {
      this.__loadCachePage(cache, page);
    }
  }

  /**
   * Creates a sub-cache for the item corresponding to the given flattened index and
   * requests the data provider to load the first page into the created sub-cache.
   * If the sub-cache already exists, the method returns immediatelly.
   *
   * @param {number} flatIndex
   */
  ensureFlatIndexHierarchy(flatIndex) {
    const { cache, item, index } = this.getFlatIndexContext(flatIndex);

    if (this.__isItemLoaded(item) && this.isExpanded(item) && !cache.getSubCache(index)) {
      const subCache = cache.createSubCache(index);
      this.__loadCachePage(subCache, 0);
    }
  }

  /**
   * Loads the first page into the root cache.
   */
  loadFirstPage() {
    this.__loadCachePage(this.rootCache, 0);
  }

  /** @private */
  __createRootCache(size) {
    return new Cache(this.__cacheContext, this.pageSize, size);
  }

  /** @private */
  __loadCachePage(cache, page) {
    if (!this.dataProvider || cache.pendingRequests[page]) {
      return;
    }

    let params = {
      page,
      pageSize: this.pageSize,
      parentItem: cache.parentItem,
    };

    if (this.dataProviderParams) {
      params = { ...params, ...this.dataProviderParams() };
    }

    const callback = (items, size) => {
      if (cache.pendingRequests[page] !== callback) {
        return;
      }

      if (size !== undefined) {
        cache.size = size;
      } else if (params.parentItem) {
        cache.size = items.length;
      }

      cache.setPage(page, items);

      this.recalculateFlatSize();

      this.dispatchEvent(new CustomEvent('page-received'));

      delete cache.pendingRequests[page];

      this.dispatchEvent(new CustomEvent('page-loaded'));
    };

    cache.pendingRequests[page] = callback;

    this.dispatchEvent(new CustomEvent('page-requested'));

    this.dataProvider(params, callback);
  }

  /** @private */
  __isItemLoaded(item) {
    if (this.isPlaceholder) {
      return !this.isPlaceholder(item);
    } else if (this.placeholder) {
      return item !== this.placeholder;
    }
    return !!item;
  }
}
