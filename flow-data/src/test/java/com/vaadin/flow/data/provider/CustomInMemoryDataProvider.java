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

import java.util.List;
import java.util.stream.Stream;

import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;

public class CustomInMemoryDataProvider<T>
        extends AbstractDataProvider<T, SerializablePredicate<T>>
        implements InMemoryDataProvider<T> {

    private List<T> items;
    private SerializablePredicate<T> filter = in -> true;
    private SerializableComparator<T> comparator;

    public CustomInMemoryDataProvider(List<T> items) {
        this.items = items;
    }

    @Override
    public SerializablePredicate<T> getFilter() {
        return filter;
    }

    @Override
    public void setFilter(SerializablePredicate<T> filter) {
        this.filter = filter;
        refreshAll();
    }

    @Override
    public SerializableComparator<T> getSortComparator() {
        return comparator;
    }

    @Override
    public void setSortComparator(SerializableComparator<T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public int size(Query<T, SerializablePredicate<T>> query) {
        return (int) items.stream().filter(filter).count();
    }

    @Override
    public Stream<T> fetch(Query<T, SerializablePredicate<T>> query) {
        Stream<T> filteredStream = items.stream().filter(filter);
        if (this.comparator != null) {
            filteredStream = filteredStream.sorted(this.comparator);
        }
        return filteredStream.skip(query.getOffset()).limit(query.getLimit());
    }
}
