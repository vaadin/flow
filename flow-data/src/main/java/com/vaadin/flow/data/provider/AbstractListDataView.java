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
 * @param <C>
 *        component type
 */
public abstract class AbstractListDataView<T, C extends Component> extends AbstractDataView<T, SerializablePredicate<T>, C>
        implements ListDataView<T, AbstractListDataView<T, C>> {

    public AbstractListDataView(C component) {
        super(component);
    }

    @Override
    public boolean hasNextItem(T item) {
        return getNextItemIndex(notNull(item), getItemsAsList()).isPresent();
    }

    @Override
    public T getNextItem(T item) {
        List<T> items = getItemsAsList();
        Optional<Integer> nextItemIndex = getNextItemIndex(notNull(item), items);
        return nextItemIndex.map(items::get).orElse(null);
    }

    @Override
    public boolean hasPreviousItem(T item) {
        return getPreviousItemIndex(notNull(item), getItemsAsList()).isPresent();
    }

    @Override
    public T getPreviousItem(T item) {
        List<T> items = getItemsAsList();
        Optional<Integer> previousItemIndex = getPreviousItemIndex(notNull(item), items);
        return previousItemIndex.map(items::get).orElse(null);
    }

    @Override
    public AbstractListDataView<T, C> withFilter(SerializablePredicate<T> filter) {
        return withFilterOrOrder(dataProvider -> {
            dataProvider.setFilter(filter);
            sizeEvent(getDataSize());
        });
    }

    @Override
    public AbstractListDataView<T, C> withSortComparator(SerializableComparator<T> sortComparator) {
        return withFilterOrOrder(dataProvider -> dataProvider.setSortComparator(sortComparator));
    }

    @Override
    public Stream<T> getAllItems() {
        return getDataProvider().getItems().stream();
    }

    @Override
    public Stream<T> getItems(SerializablePredicate<T> filter) {
        return getItems(new Query<>(filter));
    }

    @Override
    public Stream<T> getItems(Query<T, SerializablePredicate<T>> query) {
        return getDataProvider().fetch(query);
    }

    @Override
    public Stream<T> getItems() {
        return getItems(new Query<>());
    }

    @Override
    public int getDataSize() {
        return filteredItemsSize = (int) getItems().count();
    }

    @Override
    public boolean isItemPresent(T item) {
        return getItems().anyMatch(i -> Objects.equals(i, item));
    }

    @Override
    public T getItemOnIndex(int row) {
        if (row < 0) {
            throw new IndexOutOfBoundsException("Row number should be zero or greater");
        }

        List<T> filteredItems = getItemsAsList();
        if (filteredItems.isEmpty()) {
            throw new IndexOutOfBoundsException("Item requested on an empty data set");
        }
        return filteredItems.get(row);
    }

    /**
     * Provides a {@link ListDataProvider} instance of the component related to this data view.
     *
     * @return component's {@link ListDataProvider} instance
     */
    protected abstract ListDataProvider<T> getDataProvider();

    private AbstractListDataView<T, C> withFilterOrOrder(
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

    private List<T> getItemsAsList() {
        return getItems().collect(Collectors.toList());
    }

    private T notNull(T item) {
        Objects.requireNonNull(item, "Item cannot be null");
        return item;
    }
}
