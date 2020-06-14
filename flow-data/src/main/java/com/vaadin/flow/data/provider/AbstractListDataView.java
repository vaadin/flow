/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializableConsumer;
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
 *         data type
 */
public abstract class AbstractListDataView<T> extends AbstractDataView<T>
        implements ListDataView<T, AbstractListDataView<T>> {

    private static final String COLLECTION_TYPE_ERROR_MESSAGE_PATTERN =
            "DataProvider collection '%s' is not a list.";

    /**
     * Creates a new instance of {@link AbstractListDataView} subclass
     * and verifies the passed data provider is compatible with this
     * data view implementation.
     *
     * @param dataProviderSupplier
     *         supplier from which the DataProvider can be gotten
     * @param component
     *         the component that the dataView is bound to
     */
    public AbstractListDataView(
            SerializableSupplier<DataProvider<T, ?>> dataProviderSupplier,
            Component component) {
        super(dataProviderSupplier, component);
    }

    @Override
    public Optional<T> getNextItem(T item) {
        int index = getItemIndex(item);
        if (index < 0) {
            return Optional.empty();
        }
        return getItems().skip(index + 1).findFirst();
    }

    @Override
    public Optional<T> getPreviousItem(T item) {
        int index = getItemIndex(item);
        if (index <= 0) {
            return Optional.empty();
        }
        return getItems().skip(index - 1).findFirst();
    }

    @Override
    public AbstractListDataView<T> addFilter(SerializablePredicate<T> filter) {
        getDataProvider().addFilter(filter);
        return this;
    }

    @Override
    public AbstractListDataView<T> removeFilters() {
        getDataProvider().clearFilters();
        return this;
    }

    @Override
    public AbstractListDataView<T> setFilter(SerializablePredicate<T> filter) {
        return setFilterOrOrder(
                dataProvider -> dataProvider.setFilter(filter));
    }

    @Override
    public AbstractListDataView<T> setSortComparator(
            SerializableComparator<T> sortComparator) {
        return setFilterOrOrder(
                dataProvider -> dataProvider.setSortComparator(sortComparator));
    }

    @Override
    public AbstractListDataView<T> addSortComparator(
            SerializableComparator<T> sortComparator) {
        return setFilterOrOrder(
                dataProvider -> dataProvider.addSortComparator(sortComparator));
    }

    @Override
    public AbstractListDataView<T> removeSorting() {
        return setSortComparator(null);
    }

    @Override
    public <V1 extends Comparable<? super V1>> AbstractListDataView<T> setSortOrder(
            ValueProvider<T, V1> valueProvider, SortDirection sortDirection) {
        return setFilterOrOrder(
                dataProvider -> dataProvider.setSortOrder(valueProvider,
                        sortDirection));
    }

    @Override
    public <V1 extends Comparable<? super V1>> AbstractListDataView<T> addSortOrder(
            ValueProvider<T, V1> valueProvider, SortDirection sortDirection) {
        return setFilterOrOrder(
                dataProvider -> dataProvider.addSortOrder(valueProvider,
                        sortDirection));
    }

    @Override
    public boolean contains(T item) {
        return contains(item, getDataProvider());
    }

    @Override
    protected Class<?> getSupportedDataProviderType() {
        return ListDataProvider.class;
    }

    protected ListDataProvider<T> getDataProvider() {
        final DataProvider<T, ?> dataProvider = dataProviderSupplier.get();
        Objects.requireNonNull(dataProvider, "DataProvider cannot be null");
        verifyDataProviderType(dataProvider.getClass());
        return (ListDataProvider<T>) dataProvider;
    }

    @Override
    public AbstractListDataView<T> addItem(T item) {
        final ListDataProvider<T> dataProvider = getDataProvider();
        if (!contains(item, dataProvider)) {
            dataProvider.getItems().add(item);
            dataProvider.refreshAll();
        }
        return this;
    }

    @Override
    public AbstractListDataView<T> updateItem(T item) {
        ListDataProvider<T> dataProvider = getDataProvider();
        return updateItem(item, i -> getIdentifier(i, dataProvider));
    }

    @Override
    public AbstractListDataView<T> updateItem(T item,
                                SerializableFunction<T, ?> identityProvider) {
        Objects.requireNonNull(item, "Item cannot be null");
        final ListDataProvider<T> dataProvider = getDataProvider();
        Collection<T> items = dataProvider.getItems();

        if (items instanceof List) {
            final Object itemIdentifier = getIdentifier(item,
                    identityProvider);
            final List<T> itemList = (List<T>) items;

            int itemIndex = getItemIndex(item, identityProvider);

            if (itemIndex != -1) {
                T itemToUpdate = itemList.get(itemIndex);
                if (itemIdentifier.equals(
                        getIdentifier(itemToUpdate, identityProvider))) {
                    itemList.set(itemIndex, item);
                    dataProvider.refreshItem(item);
                }
            }
            return this;
        }
        throw new IllegalArgumentException(
                String.format(COLLECTION_TYPE_ERROR_MESSAGE_PATTERN,
                        items.getClass().getSimpleName()));
    }

    @Override
    public AbstractListDataView<T> addItems(Collection<T> items) {
        if (items != null && !items.isEmpty()) {
            final ListDataProvider<T> dataProvider = getDataProvider();
            Collection<T> backendItems = dataProvider.getItems();
            items.stream()
                    .filter(item ->
                            contains(item, dataProvider))
                    .forEach(item ->
                            removeItemIfPresent(item, dataProvider));
            backendItems.addAll(items);
            dataProvider.refreshAll();
        }
        return this;
    }

    @Override
    public AbstractListDataView<T> addItemAfter(T item, T after) {
        return doAddItemOnTarget(item, after,
                "Item to insert after is not available in the data",
                index -> index + 1);
    }

    @Override
    public AbstractListDataView<T> addItemBefore(T item, T before) {
        return doAddItemOnTarget(item, before,
                "Item to insert before is not available in the data",
                index -> index);
    }

    @Override
    public AbstractListDataView<T> addItemsAfter(Collection<T> items,
                                                 T after) {
        return doAddItemsOnTarget(items, after,
                "Item to insert after is not available in the data",
                (index, containsTarget) -> containsTarget ? index : index + 1);
    }

    @Override
    public AbstractListDataView<T> addItemsBefore(Collection<T> items,
                                                  T before) {
        return doAddItemsOnTarget(items, before,
                "Item to insert before is not available in the data",
                (index, containsTarget) -> index);
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
        if (items == null || items.isEmpty()) {
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
     *         item index to validate
     */
    protected void validateItemIndex(int itemIndex) {
        final int dataSize = getSize();
        if (dataSize == 0) {
            throw new IndexOutOfBoundsException(
                    String.format("Requested index %d on empty data.",
                            itemIndex));
        }
        if (itemIndex < 0 || itemIndex >= dataSize) {
            throw new IndexOutOfBoundsException(String.format(
                    "Given index %d is outside of the accepted range '0 - %d'",
                    itemIndex, dataSize - 1));
        }
    }

    private AbstractListDataView<T> setFilterOrOrder(
            SerializableConsumer<ListDataProvider<T>> filterOrOrderConsumer) {
        ListDataProvider<T> dataProvider = getDataProvider();
        filterOrOrderConsumer.accept(dataProvider);
        return this;
    }

    private int getItemIndex(
            T item, SerializableFunction<T, ?> identityProvider) {
        Objects.requireNonNull(item, "item cannot be null");
        final Object itemIdentifier = getIdentifier(item,
                identityProvider);
        AtomicInteger index = new AtomicInteger(-1);
        if (!getItems().peek(t -> index.incrementAndGet())
                .filter(t -> itemIdentifier.equals(
                        getIdentifier(t, identityProvider)))
                .findFirst().isPresent()) {
            return -1;
        }
        return index.get();
    }

    private int getItemIndex(T item, ListDataProvider<T> dataProvider) {
        return getItemIndex(item, dataProvider::getId);
    }

    private int getItemIndex(T item) {
        ListDataProvider<T> dataProvider = getDataProvider();
        return getItemIndex(item, dataProvider);
    }

    private Object getIdentifier(T item,
                                 SerializableFunction<T, ?> identityProvider) {
        Objects.requireNonNull(identityProvider,
                "Identity provider cannot be null");
        final Object itemIdentifier = identityProvider.apply(item);
        Objects.requireNonNull(itemIdentifier,
                "Identity provider should not return null");
        return itemIdentifier;
    }

    private Object getIdentifier(T item, ListDataProvider<T> dataProvider) {
        return getIdentifier(item, dataProvider::getId);
    }

    private boolean contains(T item, ListDataProvider<T> dataProvider) {
        return contains(item, dataProvider, getItems());
    }

    private boolean contains(T item, ListDataProvider<T> dataProvider,
                             Stream<T> containsIn) {
        final Object itemIdentifier = getIdentifier(item, dataProvider);
        return containsIn.anyMatch(i -> itemIdentifier.equals(
                getIdentifier(i, dataProvider)));
    }

    private void removeItemIfPresent(T item,
                                     ListDataProvider<T> dataProvider) {
        final Object itemIdentifier = getIdentifier(item, dataProvider);
        dataProvider.getItems().removeIf(i -> itemIdentifier.equals(
                getIdentifier(i, dataProvider)));
    }

    private void removeItemIfPresent(T item, ListDataProvider<T> dataProvider,
                                     List<T> from) {
        final int itemIndex = getItemIndex(item, dataProvider);
        if (itemIndex != -1) {
            from.remove(itemIndex);
        }
    }

    private boolean equals(T item, T compareTo,
                           ListDataProvider<T> dataProvider) {
        final Object itemIdentifier = getIdentifier(item, dataProvider);
        return itemIdentifier.equals(
                getIdentifier(compareTo, dataProvider));
    }

    private AbstractListDataView<T> doAddItemOnTarget(
            T item, T target, String noTargetErrMessage,
            SerializableFunction<Integer, Integer> insertItemsIndexProvider) {
        final ListDataProvider<T> dataProvider = getDataProvider();

        if (equals(item, target, dataProvider)) {
            return this;
        }

        final int targetItemIndex = getItemIndex(target, dataProvider);
        if (targetItemIndex == -1) {
            throw new IllegalArgumentException(noTargetErrMessage);
        }
        final Collection<T> backendItems = dataProvider.getItems();
        if (backendItems instanceof List) {
            final List<T> itemList = (List<T>) backendItems;
            removeItemIfPresent(item, dataProvider, itemList);
            itemList.add(insertItemsIndexProvider.apply(targetItemIndex), item);
            dataProvider.refreshAll();
            return this;
        }
        throw new IllegalArgumentException(
                String.format(COLLECTION_TYPE_ERROR_MESSAGE_PATTERN,
                        backendItems.getClass().getSimpleName()));
    }

    private AbstractListDataView<T> doAddItemsOnTarget(
            Collection<T> items, T target, String noTargetErrMessage,
        SerializableBiFunction<Integer, Boolean, Integer> insertItemsIndexProvider) {

        if (items == null || items.isEmpty()) {
            return this;
        }

        final ListDataProvider<T> dataProvider = getDataProvider();

        if (!contains(target, dataProvider)) {
            throw new IllegalArgumentException(noTargetErrMessage);
        }

        final Collection<T> backendItems = dataProvider.getItems();
        if (backendItems instanceof List) {
            final List<T> itemList = (List<T>) backendItems;
            final AtomicBoolean containsTargetItem =
                    new AtomicBoolean(false);
            items.forEach(item -> {
                if (equals(item, target, dataProvider)) {
                    /*
                     * Check and then remove the 'target' item from a backend
                     * collection in case if input items collection contains
                     * the 'target' item. Intention is to keep the order of
                     * elements as in the input collection.
                     */
                    containsTargetItem.set(true);
                } else {
                    removeItemIfPresent(item, dataProvider, itemList);
                }
            });
            int targetItemIndex = getItemIndex(target, dataProvider);

            if (containsTargetItem.get()) {
                itemList.remove(targetItemIndex);
            }

            final int indexToInsertItems = insertItemsIndexProvider.apply(
                    targetItemIndex, containsTargetItem.get());

            itemList.addAll(indexToInsertItems, items);
            dataProvider.refreshAll();
            return this;
        }
        throw new IllegalArgumentException(
                String.format(COLLECTION_TYPE_ERROR_MESSAGE_PATTERN,
                        backendItems.getClass().getSimpleName()));
    }
}
