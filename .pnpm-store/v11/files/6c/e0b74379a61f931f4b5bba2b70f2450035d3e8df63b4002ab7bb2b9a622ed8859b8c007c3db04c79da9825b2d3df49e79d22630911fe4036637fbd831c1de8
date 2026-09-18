/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { DataProviderCallback } from './data-provider-controller.js';

export type CacheContext<TItem> = { isExpanded(item: TItem): boolean };

/**
 * A class that stores items with their associated sub-caches.
 */
export class Cache<TItem> {
  /**
   * A context object.
   */
  context: CacheContext<TItem>;

  /**
   * The number of items.
   */
  size: number;

  /**
   * The number of items to display per page.
   */
  pageSize: number;

  /**
   * An array of cached items.
   */
  items: TItem[];

  /**
   * A map where the key is a requested page and the value is a callback
   * that will be called with data once the request is complete.
   */
  pendingRequests: Record<number, DataProviderCallback<TItem>>;

  /**
   * An item in the parent cache that the current cache is associated with.
   */
  get parentItem(): TItem | undefined;

  /**
   * An array of sub-caches sorted in the same order as their associated items
   * appear in the `items` array.
   */
  get subCaches(): Array<Cache<TItem>>;

  /**
   * Whether the cache or any of its descendant caches have pending requests.
   */
  get isLoading(): boolean;

  /**
   * The total number of items, including items from expanded sub-caches.
   */
  get flatSize(): number;

  /**
   * The total number of items, including items from expanded sub-caches.
   *
   * @protected
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  get effectiveSize(): number;

  constructor(
    context: CacheContext<TItem>,
    pageSize: number,
    size: number,
    parentCache?: Cache<TItem>,
    parentCacheIndex?: number,
  );

  /**
   * Recalculates the flattened size for the cache and its descendant caches recursively.
   */
  recalculateFlatSize(): void;

  /**
   * Adds an array of items corresponding to the given page
   * to the `items` array.
   */
  setPage(page: number, items: unknown[]): void;

  /**
   * Retrieves the sub-cache associated with the item at the given index
   * in the `items` array.
   */
  getSubCache(index: number): Cache<TItem> | undefined;

  /**
   * Removes the sub-cache associated with the item at the given index
   * in the `items` array.
   */
  removeSubCache(index: number): void;

  /**
   * Removes all sub-caches.
   */
  removeSubCaches(): void;

  /**
   * Creates and associates a sub-cache for the item at the given index
   * in the `items` array.
   */
  createSubCache(index: number): Cache<TItem>;

  /**
   * Retrieves the flattened index corresponding to the given index
   * of an item in the `items` array.
   */
  getFlatIndex(index: number): number;

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  getItemForIndex(index: number): TItem | undefined;

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  getCacheAndIndex(index: number): { cache: Cache<TItem>; scaledIndex: number };

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  updateSize(): void;

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  ensureSubCacheForScaledIndex(scaledIndex: number): void;

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  get grid(): HTMLElement;

  /**
   * @deprecated since 24.3 and will be removed in Vaadin 25.
   */
  get itemCaches(): object;
}
