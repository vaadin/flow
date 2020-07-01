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
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.tests.data.bean.Item;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AbstractDataViewTest {

    private Collection<Item> items;

    private ListDataProvider<Item> dataProvider;

    private AbstractDataView<Item> dataView;

    private Component component;

    @Before
    public void init() {
        items = new ArrayList<>(Arrays.asList(
                new Item(1L, "first", "description1"),
                new Item(2L, "middle", "description2"),
                new Item(3L, "last", "description3")
        ));
        dataProvider = DataProvider.ofCollection(items);
        component = new TestComponent();
        dataView = new DataViewImpl(() -> dataProvider, component);
    }

    @Test
    public void getItems_noFiltersSet_allItemsObtained() {
        Stream<Item> allItems = dataView.getItems();
        Assert.assertArrayEquals("Unexpected data set", items.toArray(),
                allItems.toArray());
    }

    @Test
    public void getItems_filtersSet_filteredItemsObtained() {
        dataProvider.setFilter(item -> item.getValue().equals("first"));
        Assert.assertArrayEquals("Unexpected data set after filtering",
                new String[] {"first"},
                dataView.getItems().map(Item::getValue).toArray());
    }

    @Test
    public void getItems_sortingSet_sortedItemsObtained() {
        dataProvider.setSortOrder(Item::getId, SortDirection.DESCENDING);
        Assert.assertArrayEquals("Unexpected items sorting",
                new Long[] {3L, 2L, 1L},
                dataView.getItems().map(Item::getId).toArray());
    }

    @Test
    public void addItemCountChangeListener_fireEvent_listenerNotified() {
        AtomicInteger fired = new AtomicInteger(0);
        dataView.addItemCountChangeListener(
                event -> fired.compareAndSet(0, event.getItemCount()));

        ComponentUtil
                .fireEvent(component, new ItemCountChangeEvent<>(component, 10));

        Assert.assertEquals(10, fired.get());
    }

    /**
     * setIdentifierProvider is tested in AbstractListDataView since it
     * has the container(T item) method.
     */

    @Tag("test-component")
    private static class TestComponent extends Component {
    }

    private static class DataViewImpl extends AbstractDataView<Item> {

        public DataViewImpl(
                SerializableSupplier<DataProvider<Item, ?>> dataProviderSupplier,
                Component component) {
            super(dataProviderSupplier, component);
        }

        @Override
        protected Class<?> getSupportedDataProviderType() {
            return DataProvider.class;
        }

        @Override
        public Item getItem(int index) {
            return null;
        }
    }

    static class CustomIdentityItemDataProvider
            extends ListDataProvider<Item> {

        public CustomIdentityItemDataProvider(Collection<Item> items) {
            super(items);
        }

        @Override
        public Object getId(Item item) {
            return item.getId();
        }
    }
}
