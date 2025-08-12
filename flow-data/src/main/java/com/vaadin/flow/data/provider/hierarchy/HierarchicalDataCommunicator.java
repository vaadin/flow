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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataChangeEvent;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.KeyMapper;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalArrayUpdater.HierarchicalUpdate;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.internal.StateNode;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Data communicator that handles requesting hierarchical data from
 * {@link HierarchicalDataProvider} and sending it to client side.
 *
 * @param <T>
 *            the bean type
 * @author Vaadin Ltd
 * @since 1.2
 */
public class HierarchicalDataCommunicator<T> extends DataCommunicator<T> {

    private final HierarchicalArrayUpdater arrayUpdater;
    private final StateNode stateNode;
    private HierarchyMapper<T, ?> mapper;
    private DataGenerator<T> dataGenerator;
    private final SerializableSupplier<ValueProvider<T, String>> uniqueKeyProviderSupplier;

    private final Map<String, HierarchicalCommunicationController<T>> dataControllers = new HashMap<>();

    /**
     * Construct a new hierarchical data communicator backed by a
     * {@link TreeDataProvider}.
     *
     * @param dataGenerator
     *            the data generator function
     * @param arrayUpdater
     *            array updater strategy
     * @param dataUpdater
     *            data updater strategy
     * @param stateNode
     *            the state node used to communicate for
     * @param uniqueKeyProviderSupplier
     *            Unique key provider for a row. If null, then using Grid's
     *            default key generator.
     */
    public HierarchicalDataCommunicator(CompositeDataGenerator<T> dataGenerator,
            HierarchicalArrayUpdater arrayUpdater,
            SerializableConsumer<JsonArray> dataUpdater, StateNode stateNode,
            SerializableSupplier<ValueProvider<T, String>> uniqueKeyProviderSupplier) {
        super(dataGenerator, arrayUpdater, dataUpdater, stateNode);
        this.dataGenerator = dataGenerator;
        this.arrayUpdater = arrayUpdater;
        this.stateNode = stateNode;
        this.uniqueKeyProviderSupplier = uniqueKeyProviderSupplier;

        KeyMapperWrapper<T> keyMapperWrapper = new KeyMapperWrapper<>();
        setKeyMapper(keyMapperWrapper);

        dataGenerator.addDataGenerator(this::generateTreeData);
        setDataProvider(new TreeDataProvider<>(new TreeData<>()), null);
    }

    private void generateTreeData(T item, JsonObject jsonObject) {
        Optional.ofNullable(mapper.getParentOfItem(item))
                .ifPresent(parent -> jsonObject.put("parentUniqueKey",
                        getKeyMapper().key(parent)));
    }

    private void requestFlush(HierarchicalUpdate update) {
        SerializableConsumer<ExecutionContext> flushRequest = context -> update
                .commit();
        stateNode.runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(stateNode, flushRequest));
    }

    private void requestFlush(HierarchicalCommunicationController<T> update) {
        SerializableConsumer<ExecutionContext> flushRequest = context -> update
                .flush();
        stateNode.runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(stateNode, flushRequest));
    }

    /**
     * Resets all the data.
     * <p>
     * It effectively resends all available data.
     */
    @Override
    public void reset() {
        super.reset();

        if (!dataControllers.isEmpty()) {
            dataControllers.values().forEach(
                    HierarchicalCommunicationController::unregisterPassivatedKeys);
            dataControllers.clear();
        }

        if (mapper != null) {
            HierarchicalUpdate update = arrayUpdater
                    .startUpdate(mapper.getRootSize());
            update.enqueue("$connector.ensureHierarchy");

            Collection<T> expandedItems = mapper.getExpandedItems();
            if (!expandedItems.isEmpty()) {
                update.enqueue("$connector.expandItems", expandedItems.stream()
                        .map(getKeyMapper()::key).map(key -> {
                            JsonObject json = Json.createObject();
                            json.put("key", key);
                            return json;
                        }).collect(JsonUtils.asArray()));
            }

            requestFlush(update);
        }
    }

    @Override
    protected void handleDataRefreshEvent(
            DataChangeEvent.DataRefreshEvent<T> event) {
        if (event.isRefreshChildren()) {
            T item = event.getItem();
            if (isExpanded(item)) {
                String parentKey = getKeyMapper().key(item);

                if (!dataControllers.containsKey(parentKey)) {
                    setParentRequestedRange(0, mapper.countChildItems(item),
                            item);
                }
                HierarchicalCommunicationController<T> dataController = dataControllers
                        .get(parentKey);
                if (dataController != null) {
                    dataController.setResendEntireRange(true);
                    requestFlush(dataController);
                }
            }
        }
        super.handleDataRefreshEvent(event);
    }

    @Override
    public Stream<T> fetchFromProvider(int offset, int limit) {
        // Instead of adding logic to this class, delegate request to the
        // separate object handling hierarchies.
        return mapper.fetchRootItems(Range.withLength(offset, limit));
    }

    /**
     * @deprecated since 24.9 and will be removed in Vaadin 25. Instead,
     *             {@link #setViewportRange(int, int)} will handle all hierarchy
     *             levels.
     */
    @Deprecated(since = "24.9", forRemoval = true)
    public void setParentRequestedRange(int start, int length, T parentItem) {
        String parentKey = getKeyMapper().key(parentItem);
        HierarchicalCommunicationController<T> controller = dataControllers
                .computeIfAbsent(parentKey,
                        key -> new HierarchicalCommunicationController<>(
                                parentKey, getKeyMapper(), mapper,
                                dataGenerator,
                                size -> arrayUpdater
                                        .startUpdate(getDataProviderSize()),
                                (pkey, range) -> mapper.fetchChildItems(
                                        getKeyMapper().get(pkey), range)));
        controller.setHasUniqueKeyProviderSupplier(
                uniqueKeyProviderSupplier.get() != null);
        Range range = computeRequestedRange(start, length);
        controller.setRequestRange(range.getStart(), range.length());
        requestFlush(controller);
    }

    @Override
    public HierarchicalDataProvider<T, ?> getDataProvider() {
        return (HierarchicalDataProvider<T, ?>) super.getDataProvider();
    }

    /**
     * Set the current hierarchical data provider for this communicator.
     *
     * @param dataProvider
     *            the data provider to set, not <code>null</code>
     * @param initialFilter
     *            the initial filter value to use, or <code>null</code> to not
     *            use any initial filter value
     *
     * @param <F>
     *            the filter type
     *
     * @return a consumer that accepts a new filter value to use
     */
    public <F> SerializableConsumer<F> setDataProvider(
            HierarchicalDataProvider<T, F> dataProvider, F initialFilter) {
        // Remove old mapper before super.setDataProvider(...) prevents calling
        // reset() before clearing the already expanded items:
        if (mapper != null) {
            mapper.destroyAllData();
        }
        mapper = new HierarchyMapper<>(dataProvider);

        SerializableConsumer<F> consumer = super.setDataProvider(dataProvider,
                initialFilter);

        // Set up mapper for requests
        mapper.setBackEndSorting(getBackEndSorting());
        mapper.setInMemorySorting(getInMemorySorting());
        mapper.setFilter(getFilter());

        return consumer;
    }

    /**
     * Set the current hierarchical data provider for this communicator.
     *
     * @param dataProvider
     *            the data provider to set, must extend
     *            {@link HierarchicalDataProvider}, not <code>null</code>
     * @param initialFilter
     *            the initial filter value to use, or <code>null</code> to not
     *            use any initial filter value
     *
     * @param <F>
     *            the filter type
     *
     * @return a consumer that accepts a new filter value to use
     */
    @Override
    public <F> SerializableConsumer<F> setDataProvider(
            DataProvider<T, F> dataProvider, F initialFilter) {
        if (dataProvider instanceof HierarchicalDataProvider) {
            return setDataProvider(
                    (HierarchicalDataProvider<T, F>) dataProvider,
                    initialFilter);
        }
        throw new IllegalArgumentException(
                "Only " + HierarchicalDataProvider.class.getName()
                        + " and subtypes supported.");
    }

    /**
     * @deprecated since 24.9 and will be removed in Vaadin 25.
     */
    @Deprecated(since = "24.9", forRemoval = true)
    public void confirmUpdate(int id, String parentKey) {
        Optional.ofNullable(dataControllers.get(parentKey))
                .ifPresent(controller -> {
                    controller.confirmUpdate(id);

                    // Not absolutely necessary, but doing it right away to
                    // release
                    // memory earlier
                    requestFlush(controller);
                });
    }

    /**
     * Collapses the given item and removes its sub-hierarchy. Calling this
     * method will have no effect if the row is already collapsed.
     * <p>
     * Changes are synchronized to the client.
     *
     * @param item
     *            the item to collapse
     */
    public void collapse(T item) {
        collapse(Arrays.asList(item));
    }

    /**
     * Collapses the given items and removes its sub-hierarchy. Calling this
     * method will have no effect if the row is already collapsed.
     * <p>
     * Changes are synchronized to the client.
     *
     * @param items
     *            the items to collapse
     * @return the collapsed items
     */
    public Collection<T> collapse(Collection<T> items) {
        List<T> collapsedItems = new ArrayList<>();
        items.forEach(item -> {
            if (mapper.collapse(item)) {
                collapsedItems.add(item);
                HierarchicalCommunicationController<T> controller = dataControllers
                        .remove(getKeyMapper().key(item));
                if (controller != null) {
                    controller.unregisterPassivatedKeys();
                }
            }
        });
        if (!collapsedItems.isEmpty()) {
            HierarchicalUpdate update = arrayUpdater
                    .startUpdate(mapper.getRootSize());
            update.enqueue("$connector.collapseItems",
                    collapsedItems.stream()
                            .map(this::generateJsonForExpandedOrCollapsedItem)
                            .collect(JsonUtils.asArray()));
            requestFlush(update);
        }
        return collapsedItems;
    }

    /**
     * Expands the given item. Calling this method will have no effect if the
     * item is already expanded or if it has no children.
     * <p>
     * Changes are synchronized to the client.
     *
     * @param item
     *            the item to expand
     */
    public void expand(T item) {
        expand(Arrays.asList(item));
    }

    /**
     * Expands the given items. Calling this method will have no effect if the
     * item is already expanded or if it has no children.
     * <p>
     * Changes are synchronized to the client.
     *
     * @param items
     *            the items to expand
     * @return the expanded items
     */
    public Collection<T> expand(Collection<T> items) {
        List<T> expandedItems = new ArrayList<>();
        items.forEach(item -> {
            if (mapper.expand(item)) {
                expandedItems.add(item);
            }
        });
        if (!expandedItems.isEmpty()) {
            HierarchicalUpdate update = arrayUpdater
                    .startUpdate(mapper.getRootSize());
            update.enqueue("$connector.expandItems",
                    expandedItems.stream()
                            .map(this::generateJsonForExpandedOrCollapsedItem)
                            .collect(JsonUtils.asArray()));
            requestFlush(update);
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
        return mapper.isExpanded(item);
    }

    /**
     * Returns depth of item in the tree starting from zero representing a root.
     *
     * @param item
     *            Target item
     * @return depth of item in the tree or -1 if item is null
     */
    public int getDepth(T item) {
        return mapper.getDepth(item);
    }

    @Override
    public int getDataProviderSize() {
        return mapper.getRootSize();
    }

    @Override
    public void setBackEndSorting(List<QuerySortOrder> sortOrder) {
        if (mapper != null) {
            mapper.setBackEndSorting(sortOrder);
        }
        super.setBackEndSorting(sortOrder);
    }

    @Override
    public void setInMemorySorting(SerializableComparator<T> comparator) {
        if (mapper != null) {
            mapper.setInMemorySorting(comparator);
        }
        super.setInMemorySorting(comparator);
    }

    protected <F> void setFilter(F filter) {
        if (mapper != null) {
            mapper.setFilter(filter);
        }
    }

    /**
     * Returns true if there is any expanded items.
     *
     * @return {@code true} if there is any expanded items.
     */
    public boolean hasExpandedItems() {
        return mapper.hasExpandedItems();
    }

    private JsonValue generateJsonForExpandedOrCollapsedItem(T item) {
        JsonObject json = Json.createObject();
        json.put("key", getKeyMapper().key(item));
        return json;
    }

    /**
     * @deprecated since 24.9 and will no longer be called in Vaadin 25.
     */
    @Override
    @Deprecated(since = "24.9", forRemoval = true)
    protected Set<String> getPassivatedKeys(Set<String> oldActive) {
        return super.getPassivatedKeys(oldActive).stream().filter(key -> {
            T item = getKeyMapper().get(key);
            if (item != null) {
                T parent = mapper.getParentOfItem(item);
                /* Short-circuit root item passivation */
                if (parent == null) {
                    return !isExpanded(item);
                }
                while (parent != null) {
                    if (!isItemActive(parent) || !isExpanded(parent)) {
                        return true;
                    }
                    parent = mapper.getParentOfItem(parent);
                }
            }
            return false;
        }).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * @deprecated since 24.9 and will no longer be called in Vaadin 25.
     */
    @Override
    @Deprecated(since = "24.9", forRemoval = true)
    protected void doUnregister(Integer updateId) {
        Set<String> passivated = passivatedByUpdate.remove(updateId);
        if (passivated != null) {
            passivated.forEach(key -> {
                T item = getKeyMapper().get(key);
                if (item != null) {
                    // If item has an active child list, do not remove it from
                    // keyMapper
                    if (!mapper.hasCurrentlyActiveChild(item)) {
                        dataGenerator.destroyData(item);
                        getKeyMapper().remove(item);
                    }
                }
            });
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
