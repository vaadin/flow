/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.data.provider.hierarchy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.vaadin.flow.function.SerializablePredicate;

/**
 * A cache for hierarchical data. Each instance of {@link Cache} represents a
 * level in the hierarchy. It is used to store the size of the level, the items,
 * and references to their child caches if they are expanded.
 * <p>
 * WARNING: This class is intended for internal use only and may change at any
 * time without notice. It is not part of the public API and should not be used
 * directly in your applications.
 *
 * @param <T>
 *            the type of items in the cache
 */
class Cache<T> implements Serializable {
    private final RootCache<T> rootCache;
    private final Cache<T> parentCache;
    private final int parentIndex;
    private int size;

    private final Map<Object, T> itemIdToItem = new HashMap<>();
    private final SortedMap<Integer, Object> indexToItemId = new TreeMap<>();
    private final SortedMap<Integer, Cache<T>> indexToCache = new TreeMap<>();

    /**
     * Creates a new cache instance with the specified parent cache, parent
     * index, and size.
     *
     * @param parentCache
     *            the parent cache, or {@code null} if this is the root cache
     * @param parentIndex
     *            the index of this cache in the parent cache
     * @param size
     *            the size of this cache
     */
    protected Cache(Cache<T> parentCache, int parentIndex, int size) {
        this.rootCache = parentCache != null ? parentCache.rootCache
                : (RootCache<T>) this;
        this.parentCache = parentCache;
        this.parentIndex = parentIndex;
        this.size = size;
    }

    /**
     * Gets the item in the parent cache that this cache is associated with.
     *
     * @return the parent item or {@code null} if there is no parent cache
     */
    public T getParentItem() {
        return parentCache != null ? parentCache.getItem(parentIndex) : null;
    }

    /**
     * Gets the depth of this cache level in the hierarchy.
     *
     * @return the depth of this cache level
     */
    public int getDepth() {
        return parentCache != null ? parentCache.getDepth() + 1 : 0;
    }

    /**
     * Sets the size of this individual cache instance.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Gets the size of this individual cache instance.
     *
     * @return the number of items stored directly in this cache level,
     *         excluding all descendant caches
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the total size of this cache, including all descendant caches.
     *
     * @return the total number of items in this cache and all its descendant
     */
    public int getFlatSize() {
        return size + indexToCache.values().stream()
                .mapToInt(Cache::getFlatSize).sum();
    }

    /**
     * Checks if this cache contains an item at the specified local index.
     *
     * @param index
     *            the index to check
     * @return {@code true} if an item is found, {@code false} otherwise
     */
    public boolean hasItem(int index) {
        return indexToItemId.containsKey(index);
    }

    /**
     * Gets the item at the specified local index in this cache.
     *
     * @param index
     *            the index of the item to retrieve
     * @return the item at the specified index, or {@code null} if not found
     */
    public T getItem(int index) {
        var itemId = indexToItemId.get(index);
        return itemIdToItem.get(itemId);
    }

    /**
     * Replaces a cached item with a new instance. Items are matched by their
     * IDs in the data provider.
     *
     * @param item
     *            the new item instance with identical ID to cached item
     */
    public void refreshItem(T item) {
        var itemId = rootCache.getItemId(item);
        itemIdToItem.replace(itemId, item);
    }

    /**
     * Sets the items in this cache starting from the specified local index.
     *
     * @param startIndex
     *            the index to start setting items
     * @param items
     *            the list of items to set
     */
    public void setItems(int startIndex, List<T> items) {
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            var itemId = rootCache.getItemId(item);
            var index = startIndex + i;

            indexToItemId.put(index, itemId);
            itemIdToItem.put(itemId, item);

            rootCache.addItemContext(item, this, index);
        }
    }

    /**
     * Removes all items and sub-caches from this cache.
     */
    public void clear() {
        indexToCache.values().forEach((cache) -> {
            cache.clear();
        });

        indexToItemId.values().forEach((itemId) -> {
            T item = itemIdToItem.get(itemId);
            rootCache.removeItemContext(item);
        });

        indexToCache.clear();
        indexToItemId.clear();
        itemIdToItem.clear();
    }

    /**
     * Checks if this cache has a sub-cache at the specified local index.
     *
     * @param index
     *            the index to check
     * @return {@code true} if a sub-cache is found, {@code false} otherwise
     */
    public boolean hasCache(int index) {
        return indexToCache.containsKey(index);
    }

    /**
     * Gets the sub-cache at the specified local index.
     *
     * @param index
     *            the index to check
     * @return the sub-cache at the specified index, or {@code null} if not
     *         found
     */
    public Cache<T> getCache(int index) {
        return indexToCache.get(index);
    }

    /**
     * Gets all sub-caches of this cache ordered by their indexes.
     *
     * @return a set of entries where the key is the index of the sub-cache and
     *         the value is the sub-cache itself
     */
    public Set<Entry<Integer, Cache<T>>> getCaches() {
        return indexToCache.entrySet();
    }

    /**
     * Creates a new sub-cache at the specified local index with the given size.
     *
     * @param index
     *            the index of the new sub-cache
     * @param size
     *            the size of the new sub-cache
     * @return the newly created sub-cache
     */
    public Cache<T> createCache(int index, int size) {
        var cache = new Cache<>(this, index, size);
        indexToCache.put(index, cache);
        return cache;
    }

    /**
     * Removes all descendant caches that match the given predicate.
     *
     * @param predicate
     *            the predicate to match caches against
     */
    public void removeDescendantCacheIf(
            SerializablePredicate<Cache<T>> predicate) {
        indexToCache.values().removeIf(cache -> {
            if (predicate.test(cache)) {
                cache.clear();
                return true;
            }
            cache.removeDescendantCacheIf(predicate);
            return false;
        });
    }

    /**
     * Maps a local cache index to its corresponding position in the flattened
     * list that includes all items from this cache and its descendants.
     * <p>
     * For example:
     *
     * <pre>
     * Cache A (current cache)
     * ├── Item 0
     * │   └── Cache B (sub cache)
     * │       ├── Item 0-0
     * │       └── Item 0-1
     * └── Item 1
     * </pre>
     *
     * In this example, the local index {@code 1} (referring to {@code Item 1}
     * in Cache A) will correspond to flat index {@code 3}.
     *
     * @param index
     *            the index of the item in this cache
     * @return the flat index of the item
     */
    public int getFlatIndex(int index) {
        return indexToCache.entrySet().stream().reduce(index, (prev, entry) -> {
            var subCacheIndex = entry.getKey();
            var subCache = entry.getValue();
            return index > subCacheIndex ? prev + subCache.getFlatSize() : prev;
        }, Integer::sum);
    }
}
