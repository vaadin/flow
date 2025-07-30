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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.vaadin.flow.function.ValueProvider;

/**
 * An extension of {@link Cache} that represents the root level in the
 * hierarchy.
 * <p>
 * WARNING: This class is intended for internal use only and may change at any
 * time without notice. It is not part of the public API and should not be used
 * directly in your applications.
 *
 * @param <T>
 *            the type of items in the cache
 */
class RootCache<T> extends Cache<T> {
    private final ValueProvider<T, Object> itemIdProvider;
    private final Map<Object, ItemContext<T>> itemIdToContext = new HashMap<>();

    public RootCache(int size, ValueProvider<T, Object> itemIdProvider) {
        super(null, -1, size);
        this.itemIdProvider = itemIdProvider;
    }

    public FlatIndexContext<T> getFlatIndexContext(int flatIndex) {
        return getFlatIndexContext(this, flatIndex);
    }

    private FlatIndexContext<T> getFlatIndexContext(Cache<T> cache,
            int flatIndex) {
        int index = flatIndex;

        for (Entry<Integer, Cache<T>> entry : cache.getCaches()) {
            var subCacheIndex = entry.getKey();
            var subCache = entry.getValue();

            if (index <= subCacheIndex) {
                break;
            }
            if (index <= subCacheIndex + subCache.getFlatSize()) {
                return getFlatIndexContext(subCache, index - subCacheIndex - 1);
            }
            index -= subCache.getFlatSize();
        }

        if (index >= cache.getSize() || index < 0) {
            return null;
        }

        return new FlatIndexContext<>(cache, index);
    }

    public int getFlatIndexByPath(int... path) {
        return getFlatIndexByPath(this, path);
    }

    private int getFlatIndexByPath(Cache<T> cache, int... path) {
        var index = path[0];
        if (index < 0) {
            index = cache.getSize() + index;
        }

        var flatIndex = cache.getFlatIndex(index);
        var subCache = cache.getCache(index);
        var restPath = Arrays.copyOfRange(path, 1, path.length);

        if (subCache != null && subCache.getFlatSize() > 0
                && restPath.length > 0) {
            return flatIndex + 1 + getFlatIndexByPath(subCache, restPath);
        }

        return flatIndex;
    }

    public ItemContext<T> getItemContext(T item) {
        Object itemId = getItemId(item);
        return itemIdToContext.get(itemId);
    }

    void addItemContext(T item, Cache<T> cache, int index) {
        Object itemId = getItemId(item);
        itemIdToContext.put(itemId, new ItemContext<>(itemId, cache, index));
    }

    void removeItemContext(T item) {
        Object itemId = getItemId(item);
        itemIdToContext.remove(itemId);
    }

    Object getItemId(T item) {
        return itemIdProvider.apply(item);
    }
}
