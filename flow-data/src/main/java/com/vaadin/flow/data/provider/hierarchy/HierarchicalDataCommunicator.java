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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.data.provider.ArrayUpdater;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataChangeEvent;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider.HierarchyFormat;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.internal.StateNode;

/**
 * WARNING: Direct use of this class in application code is not recommended and
 * may result in unexpected behavior. Use the API provided by the component
 * instead.
 * <p>
 * {@link HierarchicalDataCommunicator} is a middleware layer between
 * {@link HierarchicalDataProvider} and the client-side. It handles the loading
 * and caching of hierarchical data from the data provider based on its
 * hierarchy format, tracks expanded and collapsed items, and delivers data to
 * the client based on the {@link #setViewportRange(int, int) requested viewport
 * range}.
 * <p>
 * The communicator supports data providers that implement in one of the
 * following formats: {@link HierarchyFormat#NESTED} or
 * {@link HierarchyFormat#FLATTENED}.
 * <p>
 * <strong>Nested Hierarchy Format</strong>
 * <p>
 * When using data providers with {@link HierarchyFormat#NESTED}, the
 * communicator stores data in a hierarchical cache structure where each level
 * is represented by a {@link Cache} object, and the root by {@link RootCache}.
 * <p>
 * Before sending data to the client, the visible range is flattened into a
 * linear list. This allows the client to work with a simplified view, without
 * handling hierarchical structure directly. The {@link #getDepth(Object)}
 * method should be used by the component to get an item's depth and apply
 * indentation or other visual styling based on hierarchy level.
 * <p>
 * <strong>Flattened Hierarchy Format</strong>
 * <p>
 * When using data providers whose format is {@link HierarchyFormat#FLATTENED},
 * the communicator maintains all items in a single flat list, managed through
 * {@link RootCache}, which is directly suitable for client-side rendering
 * without any additional processing. The {@link #getDepth(Object)} method uses
 * the data provider's implementation to determine the depth of an item in the
 * hierarchy.
 * <p>
 * <strong>KeyMapper</strong>
 * <p>
 * For each item in the visible range, the communicator generates a client-side
 * key using {@link KeyMapper}. This key is used to identify the item on the
 * server when the client sends updates or interaction events for that item such
 * as selection, expansion, etc.
 *
 * @param <T>
 *            the bean type
 * @author Vaadin Ltd
 * @since 1.2
 */
public class HierarchicalDataCommunicator<T> extends DataCommunicator<T> {
    private final Set<Object> expandedItemIds = new HashSet<>();
    private final StateNode stateNode;
    private final ArrayUpdater arrayUpdater;
    private final DataGenerator<T> dataGenerator;
    private final SerializableSupplier<ValueProvider<T, String>> uniqueKeyProviderSupplier;

    private FlushRequest<T> flushRequest = null;
    private Range viewportRange = Range.withLength(0, 0);
    private int lastUpdateId = -1;

    // package private for testing purposes
    RootCache<T> rootCache;

    /**
     * Construct a new hierarchical data communicator backed by a
     * {@link TreeDataProvider}.
     *
     * @param dataGenerator
     *            the data generator function
     * @param arrayUpdater
     *            array updater strategy
     * @param stateNode
     *            the state node used to communicate for
     * @param uniqueKeyProviderSupplier
     *            Unique key provider for a row. If null, then using Grid's
     *            default key generator.
     */
    public HierarchicalDataCommunicator(CompositeDataGenerator<T> dataGenerator,
            ArrayUpdater arrayUpdater, StateNode stateNode,
            SerializableSupplier<ValueProvider<T, String>> uniqueKeyProviderSupplier) {
        // 1. Set `dataUpdater` to null, as HierarchicalDataCommunicator updates
        // refreshed items through Update#set(int, List).
        // 2. Set `fetchEnabled` to false to prevent DataCommunicator from
        // running its own fetch logic which isn't compatible with hierarchical
        // data.
        super(dataGenerator, arrayUpdater, null, stateNode, false);
        this.stateNode = stateNode;
        this.arrayUpdater = arrayUpdater;
        this.dataGenerator = dataGenerator;
        this.uniqueKeyProviderSupplier = uniqueKeyProviderSupplier;

        KeyMapperWrapper<T> keyMapperWrapper = new KeyMapperWrapper<>();
        setKeyMapper(keyMapperWrapper);

        setDataProvider(new TreeDataProvider<>(new TreeData<>()), null);

        stateNode.addAttachListener(this::requestFlush);
    }

    private FlushRequest<T> requestFlush() {
        if (flushRequest != null) {
            return flushRequest;
        }
        flushRequest = new FlushRequest<>();
        stateNode.runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(stateNode, (context) -> {
                    flush(context);
                    flushRequest = null;
                }));
        return flushRequest;
    }

    /**
     * Clears all cached data and recursively re-fetches items from hierarchy
     * levels that are still within the current viewport range, starting from
     * the root level.
     * <p>
     * WARNING: For data providers that use {@link HierarchyFormat#NESTED}, this
     * method will clear all cached hierarchy state, discarding any potential
     * information about the positions of expanded items in the hierarchy. As a
     * result, the viewport's start index may become pointing to a different
     * item if there were cached expanded items before the start index, causing
     * a shift in the currently displayed items.
     */
    @Override
    public void reset() {
        if (rootCache != null) {
            rootCache = null;
            getKeyMapper().removeAll();
            dataGenerator.destroyAllData();
        }

        requestFlush().invalidateViewport();
    }

    @Override
    protected void handleDataRefreshEvent(
            DataChangeEvent.DataRefreshEvent<T> event) {
        refresh(event.getItem(), event.isRefreshChildren());
    }

    /**
     * Replaces the cached item with a new instance and schedules a client
     * update to re-render this item.
     */
    @Override
    public void refresh(T item) {
        refresh(item, false);
    }

    /**
     * Replaces the cached item with a new instance and schedules a client
     * update to re-render this item. When {@code refreshChildren} is true, the
     * item's sub-hierarchy is cleared from the cache and scheduled to be
     * re-fetched from the data provider once visible.
     * <p>
     * WARNING: This method is only supported with data providers that use
     * {@link HierarchyFormat#NESTED} and may cause visible range shift if the
     * refreshed item contains <i>expanded</i> descendants. In such cases, they
     * might not be re-fetched immediately if they are not visible. This can
     * affect the flattened hierarchy size and result in the viewport range
     * pointing to a different set of items than before the refresh.
     *
     * @since 25.0
     * @param item
     *            the item to refresh
     * @param refreshChildren
     *            whether or not to refresh child items
     * @throws UnsupportedOperationException
     *             if {@code refreshChildren} is true and the data provider's
     *             hierarchy format is not {@link HierarchyFormat#NESTED}
     */
    public void refresh(T item, boolean refreshChildren) {
        Objects.requireNonNull(item, "Item cannot be null");

        if (!getHierarchyFormat().equals(HierarchyFormat.NESTED)
                && refreshChildren) {
            throw new UnsupportedOperationException(
                    """
                            Refreshing children of an item is only supported when the data provider \
                            uses HierarchyFormat#NESTED. For other formats, use reset() instead.
                            """);
        }

        getKeyMapper().refresh(item);
        dataGenerator.refreshData(item);

        if (rootCache == null) {
            return;
        }

        var itemContext = rootCache.getContextByItem(item);
        if (itemContext == null) {
            return;
        }

        var cache = itemContext.cache();
        var index = itemContext.index();
        cache.refreshItem(item);

        var subCache = cache.getSubCache(index);
        if (refreshChildren && subCache != null) {
            subCache.clear();
            subCache.setSize(getDataProviderChildCount(item));
            requestFlush().invalidateViewport();
        }

        requestFlush().invalidateItem(item);
    }

    @Override
    public Stream<T> fetchFromProvider(int offset, int limit) {
        return fetchDataProviderChildren(null, Range.withLength(offset, limit));
    }

    @Override
    public HierarchicalDataProvider<T, ?> getDataProvider() {
        return (HierarchicalDataProvider<T, ?>) super.getDataProvider();
    }

    /**
     * Sets the hierarchical data provider for this communicator.
     * <p>
     * The returned consumer allows updating the filter value used in subsequent
     * queries to the data provider. The consumer is only valid until another
     * data provider is set.
     *
     * @param dataProvider
     *            the hierarchical data provider to use, not {@code null}
     * @param initialFilter
     *            the initial filter value, or {@code null} if no filtering is
     *            needed
     * @param <F>
     *            the filter type
     * @return a {@link SerializableConsumer} for updating the filter value
     */
    public <F> SerializableConsumer<F> setDataProvider(
            HierarchicalDataProvider<T, F> dataProvider, F initialFilter) {
        expandedItemIds.clear();
        return super.setDataProvider(dataProvider, initialFilter);
    }

    /**
     * Alias for {@link #setDataProvider(HierarchicalDataProvider, Object)}.
     * Only hierarchical data providers are supported.
     *
     * @param dataProvider
     *            the hierarchical data provider to use, not {@code null}
     * @param initialFilter
     *            the initial filter value, or {@code null} if no filtering is
     *            needed
     * @param <F>
     *            the filter type
     * @throws IllegalArgumentException
     *             if the provided data provider is not a hierarchical data
     *             provider
     * @return a {@link SerializableConsumer} for updating the filter value
     */
    @Override
    public <F> SerializableConsumer<F> setDataProvider(
            DataProvider<T, F> dataProvider, F initialFilter) {
        if (dataProvider instanceof HierarchicalDataProvider<T, F> hierarchicalDataProvider) {
            return setDataProvider(hierarchicalDataProvider, initialFilter);
        }

        throw new IllegalArgumentException(
                "Only HierarchicalDataProvider and its subtypes are supported");
    }

    /**
     * Collapses the given item and removes its sub-hierarchy from the cache.
     * Calling this method will have no effect if the row is already collapsed.
     *
     * @param item
     *            the item to collapse
     */
    public void collapse(T item) {
        collapse(Arrays.asList(item));
    }

    /**
     * Collapses the given items and removes their sub-hierarchies from the
     * cache. Calling this method will have no effect if the row is already
     * collapsed.
     *
     * @param items
     *            the items to collapse
     * @return the collapsed items
     */
    public Collection<T> collapse(Collection<T> items) {
        var collapsedItems = items.stream().filter(
                item -> expandedItemIds.remove(getDataProvider().getId(item)))
                .toList();

        if (rootCache != null) {
            for (T item : collapsedItems) {
                var itemContext = rootCache.getContextByItem(item);
                if (itemContext == null) {
                    continue;
                }

                var cache = itemContext.cache();
                var index = itemContext.index();
                cache.removeSubCache(index);
            }
        }

        if (getHierarchyFormat().equals(HierarchyFormat.FLATTENED)) {
            reset();
        } else {
            requestFlush().invalidateViewport();
        }

        return collapsedItems;
    }

    /**
     * Expands the given item and schedules a client update to render children
     * (if visible). Calling this method will have no effect if the item is
     * already expanded or if it has no children.
     *
     * @param item
     *            the item to expand
     */
    public void expand(T item) {
        expand(Arrays.asList(item));
    }

    /**
     * Expands the given items and schedules a client update to render children
     * (if visible). Calling this method will have no effect if the item is
     * already expanded or if it has no children.
     *
     * @param items
     *            the items to expand
     * @return the expanded items
     */
    public Collection<T> expand(Collection<T> items) {
        var expandedItems = items.stream().filter(item -> {
            return hasChildren(item);
        }).filter(item -> {
            return expandedItemIds.add(getDataProvider().getId(item));
        }).toList();

        if (getHierarchyFormat().equals(HierarchyFormat.FLATTENED)) {
            reset();
        } else {
            requestFlush().invalidateViewport();
        }

        return expandedItems;
    }

    /**
     * Returns whether given item has children.
     *
     * @param item
     *            the item to test
     * @return {@code true} if item has children; {@code false} if not
     */
    public boolean hasChildren(T item) {
        return getDataProvider().hasChildren(item);
    }

    /**
     * Returns whether given item is expanded.
     *
     * @param item
     *            the item to test
     * @return {@code true} if item is expanded; {@code false} if not
     */
    public boolean isExpanded(T item) {
        Objects.requireNonNull(item, "Item cannot be null");
        return expandedItemIds.contains(getDataProvider().getId(item));
    }

    /**
     * Returns depth of item in the tree starting from zero representing a root.
     *
     * @param item
     *            Target item
     * @return depth of item in the tree or -1 if item is null or not found in
     *         the cache
     */
    public int getDepth(T item) {
        Objects.requireNonNull(item, "Item cannot be null");

        if (getHierarchyFormat().equals(HierarchyFormat.FLATTENED)) {
            return getDataProvider().getDepth(item);
        }

        if (rootCache == null) {
            return -1;
        }

        var itemContext = rootCache.getContextByItem(item);
        if (itemContext == null) {
            return -1;
        }
        return itemContext.cache().getDepth();
    }

    @Override
    public int getDataProviderSize() {
        return getDataProviderChildCount(null);
    }

    /**
     * Returns true if there is any expanded items.
     *
     * @return {@code true} if there is any expanded items.
     */
    public boolean hasExpandedItems() {
        return !expandedItemIds.isEmpty();
    }

    private JsonNode generateItemJson(T item) {
        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("key", getKeyMapper().key(item));
        dataGenerator.generateData(item, json);
        return json;
    }

    /**
     * Resolves the flat index of an item by traversing its hierarchical path
     * and resolving each level. Traversal starts from the root cache and
     * continues down until reaching the target item or a collapsed item in
     * which case its flat index is returned.
     * <p>
     * The hierarchical path is an array of indexes, each selecting a child of
     * the item at the previous index, starting from the root cache. Negative
     * indexes count from the end of the respective level. For example,
     * {@code -2} refers to the second item from the end.
     *
     * @since 25.0
     * @param path
     *            the hierarchical path to the item
     * @return the flat index of the target item after resolving all ancestors
     * @throws IndexOutOfBoundsException
     *             if any index in the path is out of bounds for its respective
     *             level
     */
    protected int resolveIndexPath(int... path) {
        ensureRootCache();
        resolveIndexPath(rootCache, path);
        return rootCache.getFlatIndexByPath(path);
    }

    private void resolveIndexPath(Cache<T> cache, int... path) {
        var depth = cache.getDepth();
        int index;
        try {
            index = cache.normalizeIndex(path[depth]);
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(
                    "Index %d is out of range for hierarchicalpath %s (depth %d, size %d)."
                            .formatted(path[depth], Arrays.toString(path),
                                    depth, cache.getSize()));
        }

        if (!cache.hasItem(index)) {
            // If the item is not found, load it from the data provider.
            preloadRange(cache, index, 1);
        }

        if (depth >= path.length - 1) {
            // If there is no rest path, we are at the target item
            return;
        }

        var item = cache.getItem(index);
        if (getHierarchyFormat().equals(HierarchyFormat.NESTED)
                && isExpanded(item)) {
            var subCache = cache.ensureSubCache(index, item, () -> {
                requestFlush().invalidateViewport();
                return getDataProviderChildCount(item);
            });
            resolveIndexPath(subCache, path);
        }
    }

    private void preloadRange(Cache<T> cache, int start, int length) {
        var range = Range.withLength(start, length)
                .restrictTo(Range.withLength(0, cache.getSize()));
        var items = fetchDataProviderChildren(cache.getParentItem(), range)
                .toList();
        cache.setItems(range.getStart(), items);
    }

    /**
     * Preloads and returns a range of items from the flattened hierarchy,
     * starting at the specified flat index and spanning the given length. Items
     * are preloaded in the backward direction, beginning from the given
     * {@code start} index and continuing toward lower indexes until the
     * specified {@code length} is reached.
     * <p>
     * NOTE: Backward preloading can affect the position of the start index in
     * the flat list, so it may need to be recalculated if it's used after this
     * method call.
     *
     * @since 25.0
     * @param start
     *            the start index of the range to preload
     * @param length
     *            the length of the range to preload
     * @return a list of items preloaded in the specified range
     */
    protected List<T> preloadFlatRangeBackward(int start, int length) {
        ensureRootCache();

        LinkedList<T> result = new LinkedList<>();
        while (result.size() < length) {
            var context = rootCache.getContextByFlatIndex(start);
            if (context == null) {
                break;
            }
            var cache = context.cache();
            var index = context.index();

            if (!cache.hasItem(index)) {
                var remainingLength = length - result.size();

                // NOTE: Subtracting 1 from the remaining length ensures
                // the item at the current index is included in the range.
                preloadRange(cache, index - (remainingLength - 1),
                        remainingLength);
            }

            var item = cache.getItem(index);

            // Checking result.size() > 0 ensures that the start item
            // won't be expanded and its descendants won't be included
            // in the result.
            if (getHierarchyFormat().equals(HierarchyFormat.NESTED)
                    && isExpanded(item) && !cache.hasSubCache(index)
                    && result.size() > 0) {
                var subCache = cache.ensureSubCache(index, item, () -> {
                    requestFlush().invalidateViewport();
                    return getDataProviderChildCount(item);
                });

                // Shift the start index to the end of the created sub-cache to
                // continue from its last item and maintain the sequential order
                // of items in the flattened list.
                start += subCache.getSize();
                continue;
            }

            start--;
            result.addFirst(item);
        }
        return result;
    }

    /**
     * Preloads and returns a range of items from the flattened hierarchy,
     * starting at the specified flat index and spanning the given length. Items
     * are preloaded in the forward direction, beginning from the given
     * {@code start} index and continuing toward higher indexes until the
     * specified {@code length} is reached.
     *
     * @since 25.0
     * @param start
     *            the start index of the range to preload
     * @param length
     *            the length of the range to preload
     * @return a list of items preloaded in the specified range
     */
    protected List<T> preloadFlatRangeForward(int start, int length) {
        ensureRootCache();

        LinkedList<T> result = new LinkedList<>();
        while (result.size() < length) {
            var context = rootCache.getContextByFlatIndex(start);
            if (context == null) {
                break;
            }
            var cache = context.cache();
            var index = context.index();

            if (!cache.hasItem(index)) {
                preloadRange(cache, index, length - result.size());
            }

            var item = cache.getItem(index);
            if (getHierarchyFormat().equals(HierarchyFormat.NESTED)
                    && isExpanded(item)) {
                cache.ensureSubCache(index, item, () -> {
                    requestFlush().invalidateViewport();
                    return getDataProviderChildCount(item);
                });
            }

            start++;
            result.addLast(item);
        }
        return result;
    }

    @Override
    public void setViewportRange(int start, int length) {
        var previousViewportRange = viewportRange;
        viewportRange = computeViewportRange(start, length);

        var partition = viewportRange.partitionWith(previousViewportRange);
        if (!partition[0].isEmpty()) {
            requestFlush().invalidateRange(partition[0]);
        }
        if (partition[1].isEmpty()) {
            requestFlush().invalidateViewport();
        }
        if (!partition[2].isEmpty()) {
            requestFlush().invalidateRange(partition[2]);
        }
    }

    private void flush(ExecutionContext context) {
        if (!context.isClientSideInitialized()) {
            reset();
            arrayUpdater.initialize();
        }

        ensureRootCache();

        if (viewportRange.getStart() >= rootCache.getFlatSize()) {
            setViewportRange(0, viewportRange.length());
        }

        var length = viewportRange.length();
        var start = viewportRange.getStart();
        var end = viewportRange.getEnd();

        var viewportItems = preloadFlatRangeForward(start, length);

        var flatSize = rootCache.getFlatSize();

        var update = arrayUpdater.startUpdate(flatSize);
        if (start > 0) {
            update.clear(0, start);
        }
        if (end < flatSize) {
            update.clear(end, flatSize - end);
        }
        for (int i = 0; i < viewportItems.size(); i++) {
            var item = viewportItems.get(i);
            var index = start + i;

            // Send updates only for items that are new in the viewport,
            // whose data has changed, or when the entire viewport needs
            // to be updated.
            if (flushRequest.isViewportInvalidated()
                    || flushRequest.isItemInvalidated(item)
                    || flushRequest.isIndexInvalidated(index)) {
                update.set(index, List.of(generateItemJson(item)));
            }
        }
        update.commit(++lastUpdateId);
    }

    /**
     * Removes no longer visible items from the cache, {@code keyMapper}, and
     * {@code dataGenerator} so that any associated client-side resources (for
     * example, DOM elements created by {@code ComponentRenderer}) can also be
     * released.
     */
    @Override
    public void confirmUpdate(int updateId) {
        if (updateId != lastUpdateId) {
            // Postpone cleanup until the most recent update is confirmed
            return;
        }

        if (rootCache == null) {
            // Nothing to clean up
            return;
        }

        HashSet<Object> viewportItemIds = new HashSet<>();
        for (int i = viewportRange.getStart(); i < viewportRange
                .getEnd(); i++) {
            var context = rootCache.getContextByFlatIndex(i);
            if (context == null) {
                continue;
            }
            var cache = context.cache();
            var index = context.index();
            if (!cache.hasItem(index)) {
                continue;
            }

            var item = cache.getItem(index);
            viewportItemIds.add(getDataProvider().getId(item));
        }

        // Remove items from the cache, keyMapper, and dataGenerator
        rootCache.removeDescendantItemIf((item) -> {
            return !viewportItemIds.contains(getDataProvider().getId(item));
        });
    }

    private HierarchyFormat getHierarchyFormat() {
        return getDataProvider().getHierarchyFormat();
    }

    private Set<Object> getExpandedItemIds() {
        return getHierarchyFormat().equals(HierarchyFormat.FLATTENED)
                ? Collections.unmodifiableSet(this.expandedItemIds)
                : Collections.emptySet();
    }

    /**
     * Creates a hierarchical query based on the given offset and limit,
     * including sorting and filtering. Depending on the data provider's
     * {@link HierarchicalDataProvider#getHierarchyFormat() hierarchy format},
     * the query fetches either just the root-level items or also their expanded
     * descendants.
     *
     * @param offset
     *            the offset of the items to fetch
     * @param limit
     *            the maximum number of items to fetch
     * @return a hierarchical query for the specified range
     */
    @Override
    public HierarchicalQuery<T, Object> buildQuery(int offset, int limit) {
        return buildQuery(null, offset, limit);
    }

    /**
     * Creates a hierarchical query based on the given parent, offset and limit,
     * including sorting and filtering. Depending on the data provider's
     * {@link HierarchicalDataProvider#getHierarchyFormat() hierarchy format},
     * the query fetches either just the direct children of the parent or also
     * their expanded descendants.
     *
     * @param parent
     *            the parent item for the query
     * @param offset
     *            the offset of the items to fetch
     * @param limit
     *            the maximum number of items to fetch
     * @return a hierarchical query for the specified range and parent
     */
    public HierarchicalQuery<T, Object> buildQuery(T parent, int offset,
            int limit) {
        return new HierarchicalQuery<>(offset, limit, getBackEndSorting(),
                getInMemorySorting(), getFilter(), getExpandedItemIds(),
                parent);
    }

    @SuppressWarnings("unchecked")
    private Stream<T> fetchDataProviderChildren(T parent, Range range) {
        var query = buildQuery(parent, range.getStart(), range.length());

        return ((HierarchicalDataProvider<T, Object>) getDataProvider())
                .fetchChildren(query).peek((item) -> {
                    if (item == null) {
                        throw new IllegalStateException(
                                "Data provider returned a null item. Null values are not supported");
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private int getDataProviderChildCount(T parent) {
        var query = new HierarchicalQuery<>(getFilter(), getExpandedItemIds(),
                parent);

        var count = ((HierarchicalDataProvider<T, Object>) getDataProvider())
                .getChildCount(query);
        if (count < 0) {
            throw new IllegalStateException(
                    "Data provider returned a negative child count. Negative values are not supported");
        }
        return count;
    }

    private RootCache<T> ensureRootCache() {
        if (rootCache == null) {
            rootCache = new RootCache<>(getDataProviderChildCount(null),
                    getDataProvider()::getId) {
                @Override
                void onItemRemoved(T item) {
                    super.onItemRemoved(item);

                    if (getKeyMapper().has(item)) {
                        dataGenerator.destroyData(item);
                        getKeyMapper().remove(item);
                    }
                }
            };
        }
        return rootCache;
    }

    private static class FlushRequest<T> implements Serializable {
        private boolean viewportInvalidated = false;
        private Set<T> invalidatedItems = new HashSet<>();
        private Set<Range> invalidatedRanges = new HashSet<>();

        public void invalidateItem(T item) {
            invalidatedItems.add(item);
        }

        public boolean isItemInvalidated(T item) {
            return invalidatedItems.contains(item);
        }

        public void invalidateRange(Range range) {
            invalidatedRanges.add(range);
        }

        public boolean isIndexInvalidated(int index) {
            return invalidatedRanges.stream()
                    .anyMatch(range -> range.contains(index));
        }

        public void invalidateViewport() {
            viewportInvalidated = true;
        }

        public boolean isViewportInvalidated() {
            return viewportInvalidated;
        }
    }

    /**
     * KeyMapper extension delegating row key creation to the
     * <code>uniqueKeyProviderSupplier</code> passed to the hierarchical data
     * communicator constructor from the component.
     * <p>
     * If <code>uniqueKeyProviderSupplier</code> is not present, this class uses
     * {@link KeyMapper#createKey()} for key creation.
     *
     * @param <V>
     *            the bean type
     */
    private class KeyMapperWrapper<V> extends KeyMapper<T> {

        private T object;

        @Override
        public String key(T o) {
            this.object = o;
            try {
                return super.key(o);
            } finally {
                this.object = null;
            }
        }

        @Override
        protected String createKey() {
            return Optional.ofNullable(uniqueKeyProviderSupplier.get())
                    .map(provider -> provider.apply(object))
                    .orElse(super.createKey());
        }
    }

    /**
     * Estimates are not supported in HierarchicalDataCommunicator
     */
    @Override
    public void setItemCountEstimate(int itemCountEstimate) {
        throw new UnsupportedOperationException(
                "Not supported in HierarchicalDataCommunicator");
    }

    /**
     * Estimates are not supported in HierarchicalDataCommunicator
     */
    @Override
    public int getItemCountEstimate() {
        throw new UnsupportedOperationException(
                "Not supported in HierarchicalDataCommunicator");
    }

    /**
     * Estimates are not supported in HierarchicalDataCommunicator
     */
    @Override
    public void setItemCountEstimateIncrease(int itemCountEstimateIncrease) {
        throw new UnsupportedOperationException(
                "Not supported in HierarchicalDataCommunicator");
    }

    /**
     * Estimates are not supported in HierarchicalDataCommunicator
     */
    @Override
    public int getItemCountEstimateIncrease() {
        throw new UnsupportedOperationException(
                "Not supported in HierarchicalDataCommunicator");
    }

    /**
     * Estimates are not supported in HierarchicalDataCommunicator
     */
    @Override
    public void setDefinedSize(boolean definedSize) {
        throw new UnsupportedOperationException(
                "Not supported in HierarchicalDataCommunicator");
    }

    /**
     * Estimates are not supported in HierarchicalDataCommunicator. Therefore
     * this method will always return {@literal true}
     */
    @Override
    public boolean isDefinedSize() {
        return true;
    }
}
