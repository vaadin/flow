/**
 * @license
 * Copyright (c) 2021 - 2024 Vaadin Ltd.
 * This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
 */

/**
 * @typedef {import('./cache.js').Cache} Cache
 */

/**
 * Returns context for the given flattened index, including:
 * - the corresponding cache
 * - the cache level
 * - the corresponding item (if loaded)
 * - the item's index in the cache's items array
 * - the page containing the item
 *
 * @param {Cache} cache
 * @param {number} flatIndex
 */
export function getFlatIndexContext(cache, flatIndex, level = 0) {
  let levelIndex = flatIndex;

  for (const subCache of cache.subCaches) {
    const index = subCache.parentCacheIndex;
    if (levelIndex <= index) {
      break;
    } else if (levelIndex <= index + subCache.flatSize) {
      return getFlatIndexContext(subCache, levelIndex - index - 1, level + 1);
    }
    levelIndex -= subCache.flatSize;
  }

  return {
    cache,
    item: cache.items[levelIndex],
    index: levelIndex,
    page: Math.floor(levelIndex / cache.pageSize),
    level,
  };
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
 *
 * @param {Cache} cache
 * @param {{ getItemId: (item: unknown) => unknown}} context
 * @param {Cache} cache
 * @param {unknown} targetItem
 * @param {number} level
 * @param {number} levelFlatIndex
 */
export function getItemContext({ getItemId }, cache, targetItem, level = 0, levelFlatIndex = 0) {
  // Start looking in this cache
  for (let index = 0; index < cache.items.length; index++) {
    const item = cache.items[index];
    if (!!item && getItemId(item) === getItemId(targetItem)) {
      return {
        cache,
        level,
        item,
        index,
        page: Math.floor(index / cache.pageSize),
        subCache: cache.getSubCache(index),
        flatIndex: levelFlatIndex + cache.getFlatIndex(index),
      };
    }
  }

  // Look through sub-caches
  for (const subCache of cache.subCaches) {
    const parentItemFlatIndex = levelFlatIndex + cache.getFlatIndex(subCache.parentCacheIndex);
    const result = getItemContext({ getItemId }, subCache, targetItem, level + 1, parentItemFlatIndex + 1);
    if (result) {
      return result;
    }
  }
}

/**
 * Recursively returns the globally flat index of the item the given indexes point to.
 * Each index in the array points to a sub-item of the previous index.
 * Using `Infinity` as an index will point to the last item on the level.
 *
 * @param {Cache} cache
 * @param {number[]} path
 * @param {number} flatIndex
 * @return {number}
 */
export function getFlatIndexByPath(cache, [levelIndex, ...subIndexes], flatIndex = 0) {
  if (levelIndex === Infinity) {
    // Treat Infinity as the last index on the level
    levelIndex = cache.size - 1;
  }

  const flatIndexOnLevel = cache.getFlatIndex(levelIndex);
  const subCache = cache.getSubCache(levelIndex);
  if (subCache && subCache.flatSize > 0 && subIndexes.length) {
    return getFlatIndexByPath(subCache, subIndexes, flatIndex + flatIndexOnLevel + 1);
  }
  return flatIndex + flatIndexOnLevel;
}
