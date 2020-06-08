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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableSupplier;

public class AbstractDataViewTest {

    private Collection<String> items;

    private ListDataProvider<String> dataProvider;

    private AbstractDataView<String> dataView;

    private Component component;

    @Before
    public void init() {

        items = new ArrayList<>(Arrays.asList("first", "middle", "last"));
        dataProvider = DataProvider.ofCollection(items);
        component = new TestComponent();
        dataView = new DataViewImpl(() -> dataProvider, component);
    }

    @Test
    public void getAllItems_noFiltersSet_allItemsObtained() {
        Stream<String> allItems = dataView.getItems();
        Assert.assertArrayEquals("Unexpected data set", items.toArray(),
                allItems.toArray());
    }

    @Test
    public void getDataSize_noFiltersSet_dataSizeObtained() {
        Assert.assertEquals("Unexpected size for data", items.size(),
                dataView.getSize());
    }

    @Test
    public void addListener_fireEvent_listenerIsCalled() {
        AtomicInteger fired = new AtomicInteger(0);
        dataView.addSizeChangeListener(
                event -> fired.compareAndSet(0, event.getSize()));

        ComponentUtil
                .fireEvent(component, new SizeChangeEvent<>(component, 10));

        Assert.assertEquals(10, fired.get());
    }

    @Tag("test-component")
    private static class TestComponent extends Component {
    }

    private static class DataViewImpl extends AbstractDataView<String> {

        public DataViewImpl(
                SerializableSupplier<DataProvider<String, ?>> dataProviderSupplier,
                Component component) {
            super(dataProviderSupplier, component);
        }

        @Override
        protected Class<?> getSupportedDataProviderType() {
            return DataProvider.class;
        }

        @Override
        public boolean contains(String item) {
            return getItems().anyMatch(item::equals);
        }
    }
}