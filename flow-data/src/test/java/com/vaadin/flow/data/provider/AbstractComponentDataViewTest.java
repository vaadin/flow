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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.shared.Registration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Abstract test class that contains the common tests for all generic
 * data view component's implementations, i.e. which extends
 * {@link AbstractDataView} and doesn't contain an in-memory or lazy
 * specific API. Concrete implementations of this class should provide a
 * particular component to be tested as a {@link HasDataView} implementation.
 */
public abstract class AbstractComponentDataViewTest {

    protected List<String> items;
    protected InMemoryDataProvider<String> dataProvider;
    protected DataView<String> dataView;
    protected HasDataView<String, ? extends DataView<String>> component;

    @Before
    public void init() {
        items = new ArrayList<>(Arrays.asList("first", "middle", "last"));
        dataProvider = new InMemoryProvider(items);
        component = getVerifiedComponent();
        dataView = component.setItems(dataProvider);
    }

    @Test
    public void getItems_noFiltersSet_allItemsObtained() {
        Stream<String> allItems = dataView.getItems();
        Assert.assertArrayEquals("Unexpected data set", items.toArray(),
                allItems.toArray());
    }

    @Test
    public void getItems_filtersSet_filteredItemsObtained() {
        dataProvider.setFilter(item -> item.equals("first"));
        Assert.assertArrayEquals("Unexpected data set after filtering",
                new String[]{"first"},
                dataView.getItems().toArray());
    }

    @Test
    public void getItems_sortingSet_sortedItemsObtained() {
        dataProvider.setSortComparator(String::compareToIgnoreCase);
        Assert.assertArrayEquals("Unexpected items sorting",
                new String[]{"first", "last", "middle"},
                dataView.getItems().toArray());
    }

    @Test
    public void getSize_filtersSet_filteredItemsObtained() {
        dataProvider.setFilter(item -> item.equals("first"));
        Assert.assertEquals("Unexpected size for data",
                1, dataView.getSize());
    }

    @Test
    public void getSize_noFiltersSet_dataSizeObtained() {
        Assert.assertEquals("Unexpected size for data", items.size(),
                dataView.getSize());
    }

    @Test
    public void addSizeChangeListener_fireEvent_listenerNotified() {
        AtomicInteger fired = new AtomicInteger(0);
        dataView.addSizeChangeListener(
                event -> fired.compareAndSet(0, event.getSize()));

        ComponentUtil.fireEvent((Component) component,
                new SizeChangeEvent<>((Component) component, 10));

        Assert.assertEquals(10, fired.get());
    }

    @Test
    public void contains_itemPresentInDataset_itemFound() {
        Assert.assertTrue("Returned false for item that should exist",
                dataView.contains("middle"));

        dataView.setIdentifierProvider(item -> item.substring(0, 1));
        Assert.assertTrue("The item was not found in the data",
                dataView.contains("fourth"));
    }

    @Test
    public void contains_itemNotPresentInDataset_itemNotFound() {
        Assert.assertFalse("Returned false for item that should exist",
                dataView.contains("non existing"));
    }

    protected abstract HasDataView<String, ? extends DataView<String>> getComponent();

    private HasDataView<String, ? extends DataView<String>> getVerifiedComponent() {
        HasDataView<String, ? extends DataView<String>> component = getComponent();
        if (component instanceof Component) {
            return component;
        }
        throw new IllegalArgumentException(String.format(
                "Component subclass is expected, but was given a '%s'",
                component.getClass().getSimpleName()));
    }

    private static class InMemoryProvider
            implements InMemoryDataProvider<String> {

        private List<String> items;
        private SerializablePredicate<String> filter = in -> true;
        private SerializableComparator<String> comparator;

        public InMemoryProvider(List<String> items) {
            this.items = items;
        }

        @Override
        public SerializablePredicate<String> getFilter() {
            return filter;
        }

        @Override
        public void setFilter(SerializablePredicate<String> filter) {
            this.filter = filter;
        }

        @Override
        public SerializableComparator<String> getSortComparator() {
            return comparator;
        }

        @Override
        public void setSortComparator(
                SerializableComparator<String> comparator) {
            this.comparator = comparator;
        }

        @Override
        public int size(Query<String, SerializablePredicate<String>> query) {
            return (int) items.stream().filter(filter).count();
        }

        @Override
        public Stream<String> fetch(
                Query<String, SerializablePredicate<String>> query) {
            Stream<String> filteredStream = items.stream().filter(filter);
            if (this.comparator != null) {
                filteredStream = filteredStream.sorted(this.comparator);
            }
            return filteredStream;
        }

        @Override
        public void refreshItem(String item) {

        }

        @Override
        public void refreshAll() {

        }

        @Override
        public Registration addDataProviderListener(
                DataProviderListener<String> listener) {
            return () -> {
            };
        }
    }

    protected static class Item {
        private long id;
        private String value;

        public Item(long id) {
            this.id = id;
        }

        public Item(long id, String value) {
            this.id = id;
            this.value = value;
        }

        public long getId() {
            return id;
        }

        public String getValue() {
            return value;
        }

        public void setId(long id) {
            this.id = id;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item item = (Item) o;
            return id == item.id &&
                    Objects.equals(value, item.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
