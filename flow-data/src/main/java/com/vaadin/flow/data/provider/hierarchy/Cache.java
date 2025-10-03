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
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.vaadin.flow.function.SerializableSupplier;

/**
 * A cache for hierarchical data. Each instance of {@link Cache} represents a
 * level in the hierarchy. It is used to store the size of the level, the items,
 * and references to their child caches if they are expanded.
 * <p>
 * WARNING: This class is intended for internal use only and may change at any
 * time without notice. It is not part of the public API and should not be used
 * directly in your applications.
 *
 * @since 25.0
 *
 * @param <T>
 *            the type of items in the cache
 */
class Cache<T> implements Serializable {
    private final RootCache<T> rootCache;
    private final Cache<T> parentCache;
    private final T parentItem;
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
     * @param parentItem
     *            the parent item, or {@code null} if this is the root cache
     * @param size
     *            the size of this cache
     */
    protected Cache(Cache<T> parentCache, T parentItem, int size) {
        this.rootCache = parentCache != null ? parentCache.rootCache
                : (RootCache<T>) this;
        this.parentCache = parentCache;
        this.parentItem = parentItem;
        this.size = size;
    }

    /**
     * Gets the parent item this cache is associated with from the parent cache.
     *
     * @return the parent item or {@code null} if this is the root cache
     */
    public T getParentItem() {
        return parentItem;
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
     * Normalizes the given index by resolving negative values as offsets from
     * the end of the cache. For example, {@code -1} refers to the last element.
     * If the resulting index does not fall within the valid range, an exception
     * is thrown.
     *
     * @param index
     *            the index to normalize
     * @return the normalized index
     * @throws IndexOutOfBoundsException
     *             if the index is out of bounds
     */
    public int normalizeIndex(int index) {
        if (index < 0) {
            index = size + index;
        }
        return Objects.checkIndex(index, size);
    }

    /**
     * Checks if this cache contains an item at the specified index.
     *
     * @param index
     *            the index to check
     * @return {@code true} if an item is found, {@code false} otherwise
     */
    public boolean hasItem(int index) {
        return indexToItemId.containsKey(index);
    }

    /**
     * Gets the item at the specified index in this cache.
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
     * Removes the item at the specified index from this cache.
     *
     * @param index
     *            the index of the item to remove
     */
    public void removeItem(int index) {
        var itemId = indexToItemId.remove(index);
        var item = itemIdToItem.remove(itemId);
        rootCache.onItemRemoved(item);
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
     * Sets the items in this cache starting from the specified index.
     *
     * @param startIndex
     *            the index to start setting items
     * @param items
     *            the list of items to set
     */
    public void setItems(int startIndex, List<T> items) {
        var index = startIndex;
        for (T item : items) {
            var itemId = rootCache.getItemId(item);

            indexToItemId.put(index, itemId);
            itemIdToItem.put(itemId, item);
            rootCache.onItemAdded(item, this, index);

            index++;
        }
    }

    /**
     * Removes all items and sub-caches from this cache and releases any
     * resources associated with them.
     */
    public void clear() {
        indexToCache.values().forEach(Cache::clear);
        indexToCache.clear();

        indexToItemId.values().forEach(itemId -> {
            rootCache.onItemRemoved(itemIdToItem.get(itemId));
        });
        indexToItemId.clear();

        itemIdToItem.clear();
    }

    /**
     * Checks if this cache has a sub-cache at the specified index.
     *
     * @param index
     *            the index to check
     * @return {@code true} if a sub-cache is found, {@code false} otherwise
     */
    public boolean hasSubCache(int index) {
        return indexToCache.containsKey(index);
    }

    /**
     * Gets the sub-cache at the specified index.
     *
     * @param index
     *            the index to check
     * @return the sub-cache at the specified index, or {@code null} if not
     *         found
     */
    public Cache<T> getSubCache(int index) {
        return indexToCache.get(index);
    }

    /**
     * Gets all sub-caches of this cache ordered by their indexes.
     *
     * @return a set of entries where the key is the index of the sub-cache and
     *         the value is the sub-cache itself
     */
    public Set<Entry<Integer, Cache<T>>> getSubCaches() {
        return indexToCache.entrySet();
    }

    /**
     * Returns a sub-cache at the specified index or creates a new one if it
     * does not exist. The new sub-cache is initialized with the size provided
     * by the given supplier.
     *
     * @param index
     *            the index of the new sub-cache
     * @param sizeSupplier
     *            a supplier that provides the size of the new sub-cache
     * @return the sub-cache instance
     */
    public Cache<T> ensureSubCache(int index, T item,
            SerializableSupplier<Integer> sizeSupplier) {
        return indexToCache.computeIfAbsent(index,
                (_key) -> new Cache<>(this, item, sizeSupplier.get()));
    }

    /**
     * Removes the sub-cache at the specified index, if it exists, and releases
     * any resources associated with it.
     *
     * @param index
     *            the index of the sub-cache to remove
     */
    public void removeSubCache(int index) {
        var subCache = indexToCache.remove(index);
        if (subCache != null) {
            subCache.clear();
        }
    }
}
