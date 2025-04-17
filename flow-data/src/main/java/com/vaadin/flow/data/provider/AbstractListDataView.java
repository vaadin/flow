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
package com.vaadin.flow.data.provider;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;

/**
 * Abstract list data view implementation which provides common methods for
 * fetching, filtering and sorting in-memory data to all {@link ListDataView}
 * subclasses.
 *
 * @param <T>
 *            data type
 */
public abstract class AbstractListDataView<T> extends AbstractDataView<T>
        implements ListDataView<T, AbstractListDataView<T>> {

    private static final String COLLECTION_TYPE_ERROR_MESSAGE_PATTERN = "DataProvider collection '%s' is not a list.";

    private static final String NULL_COLLECTION_ERROR_MESSAGE = "Items collection cannot be null";

    private final SerializableBiConsumer<SerializablePredicate<T>, SerializableComparator<T>> filterOrSortingChangedCallback;

    /**
     * Creates a new instance of {@link AbstractListDataView} subclass and
     * verifies the passed data provider is compatible with this data view
     * implementation.
     *
     * @param dataProviderSupplier
     *            supplier from which the DataProvider can be gotten
     * @param component
     *            the component that the dataView is bound to
     * @param filterOrSortingChangedCallback
     *            callback, which is being invoked when the component filtering
     *            or sorting changes, not <code>null</code>
     */
    public AbstractListDataView(
            SerializableSupplier<? extends DataProvider<T, ?>> dataProviderSupplier,
            Component component,
            SerializableBiConsumer<SerializablePredicate<T>, SerializableComparator<T>> filterOrSortingChangedCallback) {
        super(dataProviderSupplier, component);
        Objects.requireNonNull(filterOrSortingChangedCallback,
                "Filter or Sorting Change Callback cannot be empty");
        this.filterOrSortingChangedCallback = filterOrSortingChangedCallback;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int getItemCount() {
        return getDataProvider().size(DataViewUtils.getQuery(component, false));
    }

    @Override
    public T getItem(int index) {
        validateItemIndex(index);
        return getItems().skip(index).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<T> getItems() {
        return getDataProvider().fetch(DataViewUtils.getQuery(component));
    }

    @Override
    public Optional<T> getNextItem(T item) {
        int index = getItemIndex(item).orElse(-1);
        if (index < 0) {
            return Optional.empty();
        }
        return getItems().skip(index + 1).findFirst();
    }

    @Override
    public Optional<T> getPreviousItem(T item) {
        int index = getItemIndex(item).orElse(-1);
        if (index <= 0) {
            return Optional.empty();
        }
        return getItems().skip(index - 1).findFirst();
    }

    @Override
    public AbstractListDataView<T> addFilter(SerializablePredicate<T> filter) {
        Objects.requireNonNull(filter, "Filter to add cannot be null");
        SerializablePredicate<T> newFilter = DataViewUtils
                .<T> getComponentFilter(component)
                .map(originalFilter -> originalFilter.and(filter))
                .orElse(filter);
        return setFilter(newFilter);
    }

    @Override
    public AbstractListDataView<T> removeFilters() {
        return setFilter(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public AbstractListDataView<T> setFilter(SerializablePredicate<T> filter) {
        DataViewUtils.setComponentFilter(component, filter);
        fireFilteringOrSortingChangeEvent(filter,
                (SerializableComparator<T>) DataViewUtils
                        .getComponentSortComparator(component).orElse(null));
        refreshAll();
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public AbstractListDataView<T> setSortComparator(
            SerializableComparator<T> sortComparator) {
        DataViewUtils.setComponentSortComparator(component, sortComparator);
        fireFilteringOrSortingChangeEvent(
                (SerializablePredicate<T>) DataViewUtils
                        .getComponentFilter(component).orElse(null),
                sortComparator);
        refreshAll();
        return this;
    }

    @Override
    public AbstractListDataView<T> addSortComparator(
            SerializableComparator<T> sortComparator) {
        Objects.requireNonNull(sortComparator,
                "Comparator to add cannot be null");
        Optional<SerializableComparator<T>> originalComparator = DataViewUtils
                .getComponentSortComparator(component);

        if (originalComparator.isPresent()) {
            return setSortComparator((a, b) -> {
                int result = originalComparator.get().compare(a, b);
                if (result == 0) {
                    result = sortComparator.compare(a, b);
                }
                return result;
            });
        } else {
            return setSortComparator(sortComparator);
        }
    }

    @Override
    public AbstractListDataView<T> removeSorting() {
        return setSortComparator(null);
    }

    @Override
    public <V1 extends Comparable<? super V1>> AbstractListDataView<T> setSortOrder(
            ValueProvider<T, V1> valueProvider, SortDirection sortDirection) {
        SerializableComparator<T> sortComparator = InMemoryDataProviderHelpers
                .propertyComparator(valueProvider, sortDirection);
        return setSortComparator(sortComparator);
    }

    @Override
    public <V1 extends Comparable<? super V1>> AbstractListDataView<T> addSortOrder(
            ValueProvider<T, V1> valueProvider, SortDirection sortDirection) {
        SerializableComparator<T> sortComparator = InMemoryDataProviderHelpers
                .propertyComparator(valueProvider, sortDirection);
        return addSortComparator(sortComparator);
    }

    @Override
    public boolean contains(T item) {
        return getItems().anyMatch(nextItem -> equals(item, nextItem));
    }

    @Override
    protected Class<?> getSupportedDataProviderType() {
        return ListDataProvider.class;
    }

    protected ListDataProvider<T> getDataProvider() {
        final DataProvider<T, ?> dataProvider = dataProviderSupplier.get();
        Objects.requireNonNull(dataProvider, "DataProvider cannot be null");
        verifyDataProviderType(dataProvider);
        return (ListDataProvider<T>) dataProvider;
    }

    @Override
    public AbstractListDataView<T> addItem(T item) {
        final ListDataProvider<T> dataProvider = getDataProvider();
        if (!contains(item)) {
            dataProvider.getItems().add(item);
            dataProvider.refreshAll();
        }
        return this;
    }

    @Override
    public AbstractListDataView<T> addItems(Collection<T> items) {
        Objects.requireNonNull(items, NULL_COLLECTION_ERROR_MESSAGE);
        if (!items.isEmpty()) {
            final ListDataProvider<T> dataProvider = getDataProvider();
            Collection<T> backendItems = dataProvider.getItems();
            //@formatter:off
            items.stream()
                    .filter(this::contains)
                    .forEach(item ->
                            removeItemIfPresent(item, dataProvider));
            //@formatter:on
            backendItems.addAll(items);
            dataProvider.refreshAll();
        }
        return this;
    }

    @Override
    public AbstractListDataView<T> addItemAfter(T item, T after) {
        addItemOnTarget(item, after,
                "Item to insert after is not available in the data",
                index -> index + 1);
        return this;
    }

    @Override
    public AbstractListDataView<T> addItemBefore(T item, T before) {
        addItemOnTarget(item, before,
                "Item to insert before is not available in the data",
                index -> index);
        return this;
    }

    @Override
    public AbstractListDataView<T> addItemsAfter(Collection<T> items, T after) {
        addItemCollectionOnTarget(items, after,
                "Item to insert after is not available in the data",
                (index, containsTarget) -> containsTarget ? index : index + 1);
        return this;
    }

    @Override
    public AbstractListDataView<T> addItemsBefore(Collection<T> items,
            T before) {
        addItemCollectionOnTarget(items, before,
                "Item to insert before is not available in the data",
                (index, containsTarget) -> index);
        return this;
    }

    @Override
    public AbstractListDataView<T> removeItem(T item) {
        final ListDataProvider<T> dataProvider = getDataProvider();
        removeItemIfPresent(item, dataProvider);
        dataProvider.refreshAll();
        return this;
    }

    @Override
    public AbstractListDataView<T> removeItems(Collection<T> items) {
        Objects.requireNonNull(items, NULL_COLLECTION_ERROR_MESSAGE);
        if (items.isEmpty()) {
            return this;
        }
        final ListDataProvider<T> dataProvider = getDataProvider();
        items.forEach(item -> removeItemIfPresent(item, dataProvider));
        dataProvider.refreshAll();
        return this;
    }

    /**
     * Validate that index is inside bounds of the data available.
     *
     * @param itemIndex
     *            item index to validate
     */
    protected void validateItemIndex(int itemIndex) {
        final int dataSize = getItemCount();
        if (dataSize == 0) {
            throw new IndexOutOfBoundsException(String
                    .format("Requested index %d on empty data.", itemIndex));
        }
        if (itemIndex < 0 || itemIndex >= dataSize) {
            throw new IndexOutOfBoundsException(String.format(
                    "Given index %d is outside of the accepted range '0 - %d'",
                    itemIndex, dataSize - 1));
        }
    }

    private void removeItemIfPresent(T item, ListDataProvider<T> dataProvider) {
        dataProvider.getItems().removeIf(nextItem -> equals(item, nextItem));
    }

    private void addItemOnTarget(T item, T target,
            String targetItemNotFoundErrorMessage,
            SerializableFunction<Integer, Integer> insertItemsIndexProvider) {
        final ListDataProvider<T> dataProvider = getDataProvider();
        final Collection<T> backendItems = dataProvider.getItems();

        if (!(backendItems instanceof List)) {
            throw new IllegalArgumentException(
                    String.format(COLLECTION_TYPE_ERROR_MESSAGE_PATTERN,
                            backendItems.getClass().getSimpleName()));
        }

        if (equals(item, target)) {
            return;
        }

        if (!contains(target)) {
            throw new IllegalArgumentException(targetItemNotFoundErrorMessage);
        }

        final List<T> itemList = (List<T>) backendItems;
        /*
         * If the item is already present in the data provider, then it firstly
         * removed from a data provider and secondly re-added into the proper
         * position towards to target item.
         */
        removeItemIfPresent(item, dataProvider);
        itemList.add(insertItemsIndexProvider
                .apply(getItemIndex(target, itemList.stream())), item);
        dataProvider.refreshAll();
    }

    private void addItemCollectionOnTarget(Collection<T> items, T target,
            String targetItemNotFoundErrorMessage,
            SerializableBiFunction<Integer, Boolean, Integer> insertItemsIndexProvider) {
        Objects.requireNonNull(items, NULL_COLLECTION_ERROR_MESSAGE);
        if (items.isEmpty()) {
            return;
        }

        final ListDataProvider<T> dataProvider = getDataProvider();
        final Collection<T> backendItems = dataProvider.getItems();
        if (!(backendItems instanceof List)) {
            throw new IllegalArgumentException(
                    String.format(COLLECTION_TYPE_ERROR_MESSAGE_PATTERN,
                            backendItems.getClass().getSimpleName()));
        }

        if (!contains(target)) {
            throw new IllegalArgumentException(targetItemNotFoundErrorMessage);
        }

        final List<T> itemList = (List<T>) backendItems;
        /*
         * There could be a case when the items collection to be added does
         * already contain the target item. Assume a drag-and-drop case when the
         * user multi-selects a bunch of items from one component and move them
         * to another. Then, he could drag the item (among other items in the
         * bunch) which is equivalent of target item and if we do not consider
         * such a case, then the target item would be deleted and we never know
         * the position to drop the items to.
         */
        final AtomicBoolean containsTargetItem = new AtomicBoolean(false);
        items.forEach(item -> {
            /*
             * Check if an input items collection contains the target item. All
             * non-target items are deleted from backend if present, so as to be
             * placed to proper position with a proper order later on.
             */
            if (equals(target, item)) {
                containsTargetItem.set(true);
            } else {
                removeItemIfPresent(item, dataProvider);
            }
        });
        int targetItemIndex = getItemIndex(target, itemList.stream());

        /*
         * If the target item is in a collection then remove it from backend and
         * store its index so as to add an items at a desired position further.
         */
        if (containsTargetItem.get()) {
            itemList.remove(targetItemIndex);
        }

        final int indexToInsertItems = insertItemsIndexProvider
                .apply(targetItemIndex, containsTargetItem.get());

        itemList.addAll(indexToInsertItems, items);
        dataProvider.refreshAll();
    }

    private void fireFilteringOrSortingChangeEvent(
            SerializablePredicate<T> filter,
            SerializableComparator<T> sortComparator) {
        filterOrSortingChangedCallback.accept(filter, sortComparator);
    }
}
