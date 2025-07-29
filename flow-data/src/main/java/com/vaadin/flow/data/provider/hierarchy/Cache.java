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

    protected Cache(Cache<T> parentCache, int parentIndex, int size) {
        this.rootCache = parentCache != null ? parentCache.rootCache
                : (RootCache<T>) this;
        this.parentCache = parentCache;
        this.parentIndex = parentIndex;
        this.size = size;
    }

    public T getParentItem() {
        return parentCache != null ? parentCache.getItem(parentIndex) : null;
    }

    public int getDepth() {
        return parentCache != null ? parentCache.getDepth() + 1 : 0;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public int getFlatSize() {
        return size + indexToCache.values().stream()
                .mapToInt(Cache::getFlatSize).sum();
    }

    public boolean hasItem(int index) {
        return indexToItemId.containsKey(index);
    }

    public T getItem(int index) {
        var itemId = indexToItemId.get(index);
        return itemIdToItem.get(itemId);
    }

    public void refreshItem(T item) {
        var itemId = rootCache.getItemId(item);
        itemIdToItem.replace(itemId, item);
    }

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

    public boolean hasCache(int index) {
        return indexToCache.containsKey(index);
    }

    public Cache<T> getCache(int index) {
        return indexToCache.get(index);
    }

    public Set<Entry<Integer, Cache<T>>> getCaches() {
        return indexToCache.entrySet();
    }

    public Cache<T> createCache(int index, int size) {
        var cache = new Cache<>(this, index, size);
        indexToCache.put(index, cache);
        return cache;
    }

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

    public int getFlatIndex(int index) {
        return indexToCache.entrySet().stream().reduce(index, (prev, entry) -> {
            var subCacheIndex = entry.getKey();
            var subCache = entry.getValue();
            return index > subCacheIndex ? prev + subCache.getFlatSize() : prev;
        }, Integer::sum);
    }
}
