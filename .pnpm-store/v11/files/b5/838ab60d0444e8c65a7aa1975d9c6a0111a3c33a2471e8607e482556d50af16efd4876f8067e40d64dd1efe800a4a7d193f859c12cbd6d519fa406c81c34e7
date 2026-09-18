/**
 * @license
 * Copyright (c) 2021 - 2025 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */
import type { Cache } from './cache.js';

/**
 * Returns context for the given flattened index, including:
 * - the corresponding cache
 * - the cache level
 * - the corresponding item (if loaded)
 * - the item's index in the cache's items array
 * - the page containing the item
 */
export function getFlatIndexContext<TItem>(
  cache: Cache<TItem>,
  flatIndex: number,
  level: number,
): {
  cache: Cache<TItem>;
  item: TItem | undefined;
  index: number;
  page: number;
  level: number;
};

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
export function getItemContext<TItem>(
  context: { getItemId(item: TItem): unknown },
  cache: Cache<TItem>,
  targetItem: TItem,
  level: number,
  levelFlatIndex: number,
):
  | {
      level: number;
      item: TItem;
      index: number;
      page: number;
      flatIndex: number;
      cache: Cache<TItem>;
      subCache: Cache<TItem> | undefined;
    }
  | undefined;

/**
 * Recursively returns the globally flat index of the item the given indexes point to.
 * Each index in the array points to a sub-item of the previous index.
 * Using `Infinity` as an index will point to the last item on the level.
 */
export function getFlatIndexByPath<TItem>(cache: Cache<TItem>, path: number[], flatIndex: number): number;
