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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.data.provider.ArrayUpdater;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataChangeEvent;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.data.provider.hierarchy.hierarchicaldatacommunicator.Cache;
import com.vaadin.flow.data.provider.hierarchy.hierarchicaldatacommunicator.RootCache;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class HierarchicalDataCommunicator<T> extends DataCommunicator<T> {
    private final Set<Object> expandedItemIds = new HashSet<>();
    private final StateNode stateNode;
    private final ArrayUpdater arrayUpdater;
    private final DataGenerator<T> dataGenerator;

    private RootCache<T> rootCache;
    private boolean pendingFlush = false;
    private Range viewportRange;
    private int nextUpdateId = 0;

    public HierarchicalDataCommunicator(CompositeDataGenerator<T> dataGenerator,
            ArrayUpdater arrayUpdater,
            SerializableConsumer<JsonArray> dataUpdater, StateNode stateNode,
            SerializableSupplier<ValueProvider<T, String>> uniqueKeyProviderSupplier) {
        super(dataGenerator, arrayUpdater, dataUpdater, stateNode, false);
        this.stateNode = stateNode;
        this.arrayUpdater = arrayUpdater;
        this.dataGenerator = dataGenerator;

        KeyMapper<T> keyMapper = createKeyMapper(uniqueKeyProviderSupplier);
        setKeyMapper(keyMapper);

        setDataProvider(new TreeDataProvider<>(new TreeData<>()), null);

        stateNode.addAttachListener(this::onAttach);
        stateNode.addDetachListener(this::onDetach);
    }

    private void onAttach() {
        requestFlush();
    }

    private void onDetach() {

    }

    private void requestFlush() {
        if (pendingFlush) {
            return;
        }

        pendingFlush = true;
        stateNode.runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(stateNode, this::flush));
    }

    /** @see DataCommunicator#reset() */
    @Override
    public void reset() {
        getKeyMapper().removeAll();
        dataGenerator.destroyAllData();

        rootCache = null;
        requestFlush();
    }

    /** @see DataCommunicator#handleDataRefreshEvent() */
    @Override
    protected void handleDataRefreshEvent(
            DataChangeEvent.DataRefreshEvent<T> event) {
        refresh(event.getItem(), event.isRefreshChildren());
    }

    /** @see DataCommunicator#refresh() */
    @Override
    public void refresh(T item) {
        refresh(item, false);
    }

    private void refresh(T item, boolean refreshChildren) {
        Objects.requireNonNull(item, "Item cannot be null");

        getKeyMapper().refresh(item);
        dataGenerator.refreshData(item);

        if (rootCache == null) {
            return;
        }

        var itemContext = rootCache.getItemContext(item);
        if (itemContext == null) {
            return;
        }

        var cache = itemContext.cache();
        var index = itemContext.index();
        cache.refreshItem(item);

        if (refreshChildren) {
            var subCache = cache.getCache(index);
            subCache.clear();
            subCache.setSize(getDataProviderChildCount(item));
        }

        requestFlush();
    }

    /** @see DataCommunicator#setViewportRange(int, int) */
    @Override
    public void setViewportRange(int start, int length) {
        viewportRange = computeViewportRange(start, length);
        requestFlush();
    }

    /** @see DataCommunicator#confirmUpdate() */
    @Override
    public void confirmUpdate(int updateId) {
        // NO-OP
    }

    protected int resolveIndexPath(int... path) {
        var rootCache = ensureRootCache();
        resolveIndexPath(rootCache, path);
        return rootCache.getFlatIndexByPath(path);
    }

    private void resolveIndexPath(Cache<T> cache, int... path) {
        var index = path[0];

        if (index < 0) {
            // If the index is negative, it is relative to the end of the
            // cache.
            index = cache.getSize() + index;
        }

        if (!cache.hasItem(index)) {
            var items = fetchDataProviderChildren(cache.getParentItem(),
                    Range.withOnly(index)).toList();
            cache.setItems(index, items);
        }

        if (!cache.hasItem(index)) {
            return;
        }

        var item = cache.getItem(index);
        if (isExpanded(item)) {
            if (!cache.hasCache(index)) {
                cache.createCache(index, getDataProviderChildCount(item));
                requestFlush();
            }

            var subCache = cache.getCache(index);
            var restPath = Arrays.copyOfRange(path, 1, path.length);
            if (restPath.length > 0) {
                resolveIndexPath(subCache, restPath);
            }
        }
    }

    protected List<T> preloadRange(int start, int length) {
        var rootCache = ensureRootCache();

        // +1 = forward
        // -1 = backward
        var direction = Math.signum(length);

        List<T> result = new ArrayList<>();

        while (result.size() < Math.abs(length)) {
            var context = rootCache.getFlatIndexContext(start);
            if (context == null) {
                break;
            }
            var cache = context.cache();
            var index = context.index();

            if (!cache.hasItem(index)) {
                var remainingLength = Math.abs(length) - result.size();

                var range = direction > 0
                        ? Range.between(index, index + remainingLength)
                        : Range.between(index + 1 - remainingLength, index + 1);

                range = range.restrictTo(Range.withLength(0, cache.getSize()));

                var items = fetchDataProviderChildren(cache.getParentItem(),
                        range).toList();
                cache.setItems(range.getStart(), items);
            }

            var item = cache.getItem(index);

            if (isExpanded(item) && !cache.hasCache(index)
                    && (direction > 0 || result.size() > 0)) {
                var subCache = cache.createCache(index,
                        getDataProviderChildCount(item));

                if (direction < 0) {
                    start += subCache.getSize();
                    continue;
                }
            }

            if (direction > 0) {
                start++;
                result.add(item);
            } else {
                start--;
                result.add(0, item);
            }
        }

        return result;
    }

    private void flush(ExecutionContext context) {
        if (!context.isClientSideInitialized()) {
            reset();
            arrayUpdater.initialize();
        }

        var rootCache = ensureRootCache();

        if (viewportRange.getStart() >= rootCache.getFlatSize()) {
            viewportRange = Range.withLength(0, viewportRange.length());
        }

        var length = viewportRange.length();
        var start = viewportRange.getStart();
        var end = viewportRange.getEnd();

        var result = preloadRange(start, length);

        var flatSize = rootCache.getFlatSize();

        var update = arrayUpdater.startUpdate(flatSize);
        if (start > 0) {
            update.clear(0, start);
        }
        update.clear(end, flatSize - end);
        update.set(start, result.stream().map(this::generateItemJson).toList());
        update.commit(nextUpdateId++);

        pendingFlush = false;
    }

    /** @see HierarchicalDataCommunicator#hasChildren(T) */
    public boolean hasChildren(T item) {
        return getDataProvider().hasChildren(item);
    }

    /** @see HierarchicalDataCommunicator#getDepth(T) */
    public int getDepth(T item) {
        if (rootCache == null) {
            return -1;
        }

        var itemContext = rootCache.getItemContext(item);
        if (itemContext == null) {
            return -1;
        }
        return itemContext.cache().getDepth();
    }

    /** @see HierarchicalDataCommunicator#isExpanded(T) */
    public boolean isExpanded(T item) {
        if (item == null) {
            // Root nodes are always visible.
            return true;
        }
        return expandedItemIds.contains(getDataProvider().getId(item));
    }

    /** @see HierarchicalDataCommunicator#expand(T item) */
    public void expand(T item) {
        expand(Arrays.asList(item));
    }

    /** @see HierarchicalDataCommunicator#expand(Collection) */
    public Collection<T> expand(Collection<T> items) {
        var expandedItems = items.stream().filter(item -> {
            if (!hasChildren(item)) {
                return false;
            }

            return expandedItemIds.add(getDataProvider().getId(item));
        }).toList();

        requestFlush();

        return expandedItems;
    }

    /** @see HierarchicalDataCommunicator#collapse(T) */
    public void collapse(T item) {
        collapse(Arrays.asList(item));
    }

    /** @see HierarchicalDataCommunicator#collapse(Collection) */
    public Collection<T> collapse(Collection<T> items) {
        var collapsedItems = items.stream().filter(
                item -> expandedItemIds.remove(getDataProvider().getId(item)))
                .toList();

        if (rootCache != null) {
            rootCache.removeDescendantCacheIf(
                    (cache) -> !isExpanded(cache.getParentItem()));
            requestFlush();
        }

        return collapsedItems;
    }

    private JsonValue generateItemJson(T item) {
        JsonObject json = Json.createObject();
        json.put("key", getKeyMapper().key(item));
        dataGenerator.generateData(item, json);
        return json;
    }

    private KeyMapper<T> createKeyMapper(
            SerializableSupplier<ValueProvider<T, String>> uniqueKeyProviderSupplier) {
        return new KeyMapper<T>() {
            private T object;

            @Override
            public String key(T o) {
                object = o;
                try {
                    return super.key(o);
                } finally {
                    object = null;
                }
            }

            @Override
            protected String createKey() {
                var uniqueKeyProvider = uniqueKeyProviderSupplier.get();
                return uniqueKeyProvider != null
                        ? uniqueKeyProvider.apply(object)
                        : super.createKey();
            }
        };
    }

    private RootCache<T> ensureRootCache() {
        if (rootCache == null) {
            rootCache = new RootCache<>(getDataProviderChildCount(null),
                    getDataProvider()::getId) {
                @Override
                protected void removeItemContext(T item) {
                    super.removeItemContext(item);

                    getKeyMapper().remove(item);
                    dataGenerator.destroyData(item);
                }
            };
        }
        return rootCache;
    }

    @Override
    public HierarchicalDataProvider<T, ?> getDataProvider() {
        return (HierarchicalDataProvider<T, ?>) super.getDataProvider();
    }

    @Override
    public <F> SerializableConsumer<F> setDataProvider(
            DataProvider<T, F> dataProvider, F initialFilter) {
        return setDataProvider(asHierarchicalDataProvider(dataProvider),
                initialFilter);
    }

    @Override
    public <F> SerializableConsumer<Filter<F>> setDataProvider(
            DataProvider<T, F> dataProvider, F initialFilter,
            boolean notifiesOnChange) {
        return setDataProvider(asHierarchicalDataProvider(dataProvider),
                initialFilter, notifiesOnChange);
    }

    public <F> SerializableConsumer<F> setDataProvider(
            HierarchicalDataProvider<T, F> dataProvider, F initialFilter) {
        return super.setDataProvider(dataProvider, initialFilter);
    }

    public <F> SerializableConsumer<Filter<F>> setDataProvider(
            HierarchicalDataProvider<T, F> dataProvider, F initialFilter,
            boolean notifiesOnChange) {
        expandedItemIds.clear();

        return super.setDataProvider(dataProvider, initialFilter,
                notifiesOnChange);
    }

    /** @see DataCommunicator#getDataProviderSize() */
    @Override
    public int getDataProviderSize() {
        return getDataProviderChildCount(null);
    }

    /** @see DataCommunicator#fetchFromProvider() */
    @Override
    public Stream<T> fetchFromProvider(int offset, int limit) {
        return fetchDataProviderChildren(null, Range.withLength(offset, limit));
    }

    @SuppressWarnings("unchecked")
    private Stream<T> fetchDataProviderChildren(T parent, Range range) {
        var query = new HierarchicalQuery<>(range.getStart(), range.length(),
                getBackEndSorting(), getInMemorySorting(), getFilter(), parent);

        return ((HierarchicalDataProvider<T, Object>) getDataProvider())
                .fetchChildren(query);
    }

    @SuppressWarnings("unchecked")
    private int getDataProviderChildCount(T parent) {
        var query = new HierarchicalQuery<>(getFilter(), parent);

        return ((HierarchicalDataProvider<T, Object>) getDataProvider())
                .getChildCount(query);
    }

    private <F> HierarchicalDataProvider<T, F> asHierarchicalDataProvider(
            DataProvider<T, F> dataProvider) {
        if (dataProvider instanceof HierarchicalDataProvider<T, F> hierarchicalDataProvider) {
            return hierarchicalDataProvider;
        }

        throw new IllegalArgumentException(
                "Only HierarchicalDataCommunicator and its subtypes are supported");
    }
}
