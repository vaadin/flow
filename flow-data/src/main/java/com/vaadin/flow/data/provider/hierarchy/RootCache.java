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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;

/**
 * An extension of {@link Cache} that represents the root level in the
 * hierarchy.
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
class RootCache<T> extends Cache<T> {
    private final ValueProvider<T, Object> itemIdProvider;
    private final Map<Object, ItemContext<T>> itemIdToContext = new HashMap<>();

    /**
     * Creates a new root cache instance with the specified size and item ID
     * provider.
     *
     * @param size
     *            the size of the cache
     * @param itemIdProvider
     *            the item ID provider
     */
    public RootCache(int size, ValueProvider<T, Object> itemIdProvider) {
        super(null, null, size);
        this.itemIdProvider = itemIdProvider;
    }

    /**
     * Gets the total size of the hierarchy (including all descendant caches).
     *
     * @return the total number of items in this cache and all its descendant
     */
    public int getFlatSize() {
        return getFlatSize(this);
    }

    private int getFlatSize(Cache<T> cache) {
        return cache.getSize() + cache.getSubCaches().stream()
                .mapToInt((entry) -> getFlatSize(entry.getValue())).sum();
    }

    /**
     * Retrieves the hierarchical context for an item by its position in the
     * flattened view of the entire hierarchy. The result includes a reference
     * to the cache that contains the item and the item's local index within
     * that cache.
     *
     * @param flatIndex
     *            the flat index to get the context for
     * @return an {@link ItemContext} record, or {@code null} if not found
     */
    public ItemContext<T> getContextByFlatIndex(int flatIndex) {
        return getContextByFlatIndex(this, flatIndex);
    }

    private ItemContext<T> getContextByFlatIndex(Cache<T> cache,
            int localFlatIndex) {
        int index = localFlatIndex;

        for (Entry<Integer, Cache<T>> entry : cache.getSubCaches()) {
            var subCache = entry.getValue();
            var subCacheIndex = entry.getKey();
            var subCacheFlatSize = getFlatSize(subCache);

            if (index <= subCacheIndex) {
                break;
            }
            if (index <= subCacheIndex + subCacheFlatSize) {
                return getContextByFlatIndex(subCache,
                        index - subCacheIndex - 1);
            }
            index -= subCacheFlatSize;
        }

        if (index >= cache.getSize() || index < 0) {
            return null;
        }

        return new ItemContext<>(cache, index);
    }

    /**
     * Retrieves the position of an item in the flattened view of the entire
     * hierarchy by following its hierarchical path.
     * <p>
     * The path is an array of integers, where each integer represents the index
     * of a child item within its parent's sub-cache. Traversal starts at the
     * root cache, using the first integer to select an item. The next integer
     * is then used to select a child of that item, and so on — each step going
     * one level deeper in the hierarchy.
     *
     * @param path
     *            the path to the item
     * @return the flat index of the item, or -1 if not found
     */
    public int getFlatIndexByPath(int... path) {
        return getFlatIndexByPath(this, path);
    }

    private int getFlatIndexByPath(Cache<T> cache, int... path) {
        var restPath = Arrays.copyOfRange(path, 1, path.length);

        var index = Math.min(path[0], cache.getSize() - 1);
        if (index < 0) {
            // Negative index means counting from the end
            index = Math.max(cache.getSize() + index, 0);
        }

        var flatIndex = flattenIndex(cache, index);
        var subCache = cache.getSubCache(index);
        if (subCache != null && getFlatSize(subCache) > 0
                && restPath.length > 0) {
            return flatIndex + 1 + getFlatIndexByPath(subCache, restPath);
        }

        return flatIndex;
    }

    /**
     * Converts a local cache index into its corresponding position in the
     * flattened list that includes all items from this cache and its
     * descendants.
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
    private int flattenIndex(Cache<T> cache, int index) {
        return cache.getSubCaches().stream().reduce(index, (prev, entry) -> {
            var subCacheIndex = entry.getKey();
            var subCache = entry.getValue();
            return index > subCacheIndex ? prev + getFlatSize(subCache) : prev;
        }, Integer::sum);
    }

    /**
     * Retrieves the hierarchical context for the specified item. The result
     * includes the item's ID, a reference to the cache that contains the item
     * and the item's local index within that cache.
     *
     * @param item
     *            the item to get the context for
     * @return an {@link ItemContext} record, or {@code null} if not found
     */
    public ItemContext<T> getContextByItem(T item) {
        Object itemId = getItemId(item);
        return itemIdToContext.get(itemId);
    }

    /**
     * Removes all descendant items that match the given predicate.
     *
     * @param predicate
     *            the predicate to match items against
     */
    public void removeDescendantItemIf(SerializablePredicate<T> predicate) {
        itemIdToContext.values().stream().filter((itemContext) -> {
            var cache = itemContext.cache();
            var index = itemContext.index();
            var item = cache.getItem(index);
            return predicate.test(item);
        }).toList().forEach((itemContext) -> {
            var cache = itemContext.cache();
            var index = itemContext.index();
            cache.removeItem(index);
        });
    }

    void onItemAdded(T item, Cache<T> cache, int index) {
        Object itemId = getItemId(item);
        itemIdToContext.put(itemId, new ItemContext<>(cache, index));
    }

    void onItemRemoved(T item) {
        Object itemId = getItemId(item);
        itemIdToContext.remove(itemId);
    }

    Object getItemId(T item) {
        return itemIdProvider.apply(item);
    }

    /**
     * A record that includes a reference to the cache that contains the item
     * and the item's local index within that cache.
     *
     * @param <T>
     *            the type of items in the cache
     */
    static record ItemContext<T>(Cache<T> cache,
            int index) implements Serializable {
    }
}
