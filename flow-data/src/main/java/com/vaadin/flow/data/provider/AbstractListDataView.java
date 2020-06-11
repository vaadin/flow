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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
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
        final ListDataProvider<T> dataProvider = getDataProvider();
        final Object itemIdentifier = getIdentifier(item, dataProvider);
        return getItems().anyMatch(i -> itemIdentifier.equals(
                getIdentifier(i, dataProvider)));
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
        if (!contains(item)) {
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
        final ListDataProvider<T> dataProvider = getDataProvider();
        Collection<T> backendItems = dataProvider.getItems();
        if (items != null && !items.isEmpty()) {
            items.stream()
                    .filter(item ->
                            !contains(item))
                    .forEach(backendItems::add);
            dataProvider.refreshAll();
        }
        return this;
    }

    @Override
    public AbstractListDataView<T> addItemAfter(T item, T after) {
        final Collection<T> backendItems = getDataProvider().getItems();
        final int afterItemIndex = getItemIndex(after);
        if (afterItemIndex == -1) {
            throw new IllegalArgumentException(
                    "Item to insert after is not available in the data");
        }

        // Do nothing if the backend collection already contains such an item
        if (contains(item)) {
            return this;
        }

        if (backendItems instanceof List) {
            final List<T> itemList = (List<T>) backendItems;
            itemList.add(afterItemIndex + 1, item);
            getDataProvider().refreshAll();
            return this;
        }
        throw new IllegalArgumentException(
                String.format(COLLECTION_TYPE_ERROR_MESSAGE_PATTERN,
                        backendItems.getClass().getSimpleName()));
    }

    @Override
    public AbstractListDataView<T> addItemsAfter(Collection<T> items, T after) {
        if (items == null || items.isEmpty()) {
            return this;
        }
        final Collection<T> backendItems = getDataProvider().getItems();
        final int afterItemIndex = getItemIndex(after);
        if (afterItemIndex == -1) {
            throw new IllegalArgumentException(
                    "Item to insert after is not available in the data");
        }
        if (backendItems instanceof List) {
            final List<T> itemList = (List<T>) backendItems;
            final List<T> itemsToAdd = items.stream()
                    .filter(item ->
                            !contains(item))
                    .collect(Collectors.toList());
            itemList.addAll(afterItemIndex + 1, itemsToAdd);
            getDataProvider().refreshAll();
            return this;
        }
        throw new IllegalArgumentException(
                String.format(COLLECTION_TYPE_ERROR_MESSAGE_PATTERN,
                        backendItems.getClass().getSimpleName()));
    }

    @Override
    public AbstractListDataView<T> addItemBefore(T item, T before) {
        final Collection<T> backendItems = getDataProvider().getItems();
        final int beforeItemIndex = getItemIndex(before);
        if (beforeItemIndex == -1) {
            throw new IllegalArgumentException(
                    "Item to insert before is not available in the data");
        }

        // Do nothing if the backend collection already contains such an item
        if (contains(item)) {
            return this;
        }

        if (backendItems instanceof List) {
            final List<T> itemList = (List<T>) backendItems;
            itemList.add(beforeItemIndex, item);
            getDataProvider().refreshAll();
            return this;
        }
        throw new IllegalArgumentException(
                String.format(COLLECTION_TYPE_ERROR_MESSAGE_PATTERN,
                        backendItems.getClass().getSimpleName()));
    }

    @Override
    public AbstractListDataView<T> addItemsBefore(Collection<T> items,
            T before) {
        if (items == null || items.isEmpty()) {
            return this;
        }
        final Collection<T> backendItems = getDataProvider().getItems();
        final int beforeItemIndex = getItemIndex(before);
        if (beforeItemIndex == -1) {
            throw new IllegalArgumentException(
                    "Item to insert before is not available in the data");
        }
        if (backendItems instanceof List) {
            final List<T> itemList = (List<T>) backendItems;
            final List<T> itemsToAdd = items.stream()
                    .filter(item ->
                            !contains(item))
                    .collect(Collectors.toList());
            itemList.addAll(beforeItemIndex, itemsToAdd);
            getDataProvider().refreshAll();
            return this;
        }
        throw new IllegalArgumentException(
                String.format(COLLECTION_TYPE_ERROR_MESSAGE_PATTERN,
                        backendItems.getClass().getSimpleName()));
    }

    @Override
    public AbstractListDataView<T> removeItem(T item) {
        final ListDataProvider<T> dataProvider = getDataProvider();
        final Object itemIdentifier = getIdentifier(item, dataProvider);
        dataProvider.getItems().removeIf(i -> itemIdentifier.equals(
                        getIdentifier(i, dataProvider)));
        dataProvider.refreshAll();
        return this;
    }

    @Override
    public AbstractListDataView<T> removeItems(Collection<T> items) {
        if (items == null || items.isEmpty()) {
            return this;
        }
        final ListDataProvider<T> dataProvider = getDataProvider();
        Collection<T> backendItems = dataProvider.getItems();
        items.forEach(item -> {
            Object itemIdentifier = getIdentifier(item, dataProvider);
            backendItems.removeIf(i -> itemIdentifier.equals(
                    getIdentifier(i, dataProvider)));
        });
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

    private int getItemIndex(T item) {
        ListDataProvider<T> dataProvider = getDataProvider();
        return getItemIndex(item, dataProvider::getId);
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
}
