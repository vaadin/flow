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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.SerializableSupplier;

import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract list data view implementation which provides common methods
 * for fetching, filtering and sorting in-memory data to all {@link ListDataView} subclasses.
 *
 * @param <T>
 *        data type
 */
public abstract class AbstractListDataView<T> extends AbstractDataView<T>
        implements ListDataView<T, AbstractListDataView<T>> {

    public AbstractListDataView(
            SerializableSupplier<DataProvider<T, ?>> dataProviderSupplier,
            SerializableSupplier<? extends Component> componentSupplier) {
        super(dataProviderSupplier, componentSupplier);
    }

    @Override
    public boolean hasNextItem(T item) {
        return getNextItemIndex(notNull(item), getAllItemsAsList()).isPresent();
    }

    @Override
    public T getNextItem(T item) {
        List<T> items = getAllItemsAsList();
        Optional<Integer> nextItemIndex = getNextItemIndex(notNull(item), items);
        return nextItemIndex.map(items::get).orElse(null);
    }

    @Override
    public boolean hasPreviousItem(T item) {
        return getPreviousItemIndex(notNull(item), getAllItemsAsList()).isPresent();
    }

    @Override
    public T getPreviousItem(T item) {
        List<T> items = getAllItemsAsList();
        Optional<Integer> previousItemIndex = getPreviousItemIndex(notNull(item), items);
        return previousItemIndex.map(items::get).orElse(null);
    }

    @Override
    public AbstractListDataView<T> withFilter(SerializablePredicate<T> filter) {
        return withFilterOrOrder(dataProvider -> dataProvider.setFilter(filter));
    }

    @Override
    public AbstractListDataView<T> withSortComparator(SerializableComparator<T> sortComparator) {
        return withFilterOrOrder(dataProvider -> dataProvider.setSortComparator(sortComparator));
    }

    @Override
    public boolean isItemPresent(T item) {
        // TODO: delegate this to the data communicator/component, since the equality could be
        //  determined by the provided identity checker (the default is equals).
        return getAllItems().anyMatch(i -> Objects.equals(i, item));
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
        dataProvider.getItems().add(item);
        dataProvider.refreshAll();
        return this;
    }

    @Override
    public AbstractListDataView<T> removeItem(T item) {
        final ListDataProvider<T> dataProvider = getDataProvider();
        dataProvider.getItems().remove(item);
        dataProvider.refreshAll();
        return this;
    }

    protected List<T> getAllItemsAsList() {
        return getAllItems().collect(Collectors.toList());
    }

    private AbstractListDataView<T> withFilterOrOrder(
            SerializableConsumer<ListDataProvider<T>> filterOrOrderConsumer) {
        ListDataProvider<T> dataProvider = getDataProvider();
        filterOrOrderConsumer.accept(dataProvider);
        return this;
    }

    private Optional<Integer> getNextItemIndex(T item, List<T> items) {
        int itemIndex = items.indexOf(item);
        return (itemIndex != -1 && itemIndex < items.size() - 1) ? Optional.of(itemIndex + 1) : Optional.empty();
    }

    private Optional<Integer> getPreviousItemIndex(T item, List<T> items) {
        int itemIndex = items.indexOf(item);
        return (itemIndex > 0) ? Optional.of(itemIndex - 1) : Optional.empty();
    }

    private T notNull(T item) {
        Objects.requireNonNull(item, "Item cannot be null");
        return item;
    }
}
