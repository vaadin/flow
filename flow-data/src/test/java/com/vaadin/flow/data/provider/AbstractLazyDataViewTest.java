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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.tests.data.bean.Item;

import tools.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AbstractLazyDataViewTest {

    private static final String ITEM1 = "foo";

    @Tag("test-component")
    private static class TestComponent extends Component {
    }

    private ListDataProvider<String> badProvider;
    private DataProvider<String, Void> dataProvider;
    private DataCommunicator<String> dataCommunicator;
    private AbstractLazyDataView<String> dataView;
    private Component component;
    private DataCommunicatorTest.MockUI ui;
    @Mock
    private ArrayUpdater arrayUpdater;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        component = new TestComponent();
        ui = new DataCommunicatorTest.MockUI();
        ui.add(component);

        ArrayUpdater.Update update = new ArrayUpdater.Update() {

            @Override
            public void clear(int start, int length) {

            }

            @Override
            public void set(int start, List<JsonNode> items) {

            }

            @Override
            public void commit(int updateId) {

            }
        };

        Mockito.when(arrayUpdater.startUpdate(Mockito.anyInt()))
                .thenReturn(update);

        badProvider = DataProvider.ofItems("foo", "bar");
        dataProvider = DataProvider.fromCallbacks(query -> {
            // this is a stupid limitation and could maybe be removed
            query.getOffset();
            query.getLimit();
            return Stream.of(ITEM1, "bar", "baz");
        }, query -> 3);
        dataCommunicator = new DataCommunicator<>((item, jsonObject) -> {
        }, arrayUpdater, null, component.getElement().getNode());
        // need to set a lazy data provider to communicator or type check fails
        dataCommunicator.setDataProvider(dataProvider, null);
        dataView = new AbstractLazyDataView<String>(dataCommunicator,
                component) {
        };
    }

    @Test
    public void defaults_withCorrectDataProvider_noErrors() {
        dataCommunicator.setDataProvider(dataProvider, null);
        Assert.assertTrue(dataView.getDataCommunicator().isDefinedSize());
        Assert.assertEquals(BackEndDataProvider.class,
                dataView.getSupportedDataProviderType());
        Assert.assertEquals(3, dataView.getDataCommunicator().getItemCount());
        Assert.assertEquals(200, dataView.getItemCountEstimate());
        Assert.assertEquals(200, dataView.getItemCountEstimateIncrease());

        dataView.setItemCountUnknown();
        Assert.assertFalse(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountFromDataProvider();
        Assert.assertTrue(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountEstimate(500);
        Assert.assertFalse(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountFromDataProvider();
        Assert.assertTrue(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountEstimateIncrease(200);
        Assert.assertFalse(dataView.getDataCommunicator().isDefinedSize());
    }

    @Test
    public void dataViewCreated_beforeSettingDataProvider_verificationPassed() {
        // Data provider verification should pass even if the developer
        // hasn't setup any data provider to a component. In the example
        // below, we just create a data communicator instance but don't call
        // 'setDataProvider' method.
        new AbstractLazyDataView<String>(
                new DataCommunicator<>((item, jsonObject) -> {
                }, null, null, component.getElement().getNode()), component) {
        };
    }

    @Test
    public void addItemCountListener_beforeSettingDataProvider_verificationPassed() {
        AbstractLazyDataView<String> dataView = new AbstractLazyDataView<String>(
                new DataCommunicator<>((item, jsonObject) -> {
                }, null, null, component.getElement().getNode()), component) {
        };

        // Check that we can add a listener even if no data provider set by
        // user
        dataView.addItemCountChangeListener(event -> {
        });
    }

    @Test(expected = IllegalStateException.class)
    public void dataViewAPIUsed_beforeSettingDataProvider_throws() {
        AbstractLazyDataView<String> dataView = new AbstractLazyDataView<String>(
                new DataCommunicator<>((item, jsonObject) -> {
                }, null, null, component.getElement().getNode()), component) {
        };

        // Check that the verification not passed for any 'lazy load'
        // specific method
        dataView.setItemCountUnknown();
    }

    @Test(expected = IllegalStateException.class)
    public void existingDataView_dataProviderIsChangedToInMemory_throws() {
        dataCommunicator.setDataProvider(badProvider, null);
        // any method call should be enough to trigger the check for type
        dataView.setItemCountUnknown();
    }

    @Test
    public void itemCount_definedItemCount() {
        final AtomicInteger itemCount = new AtomicInteger(0);
        dataView.addItemCountChangeListener(
                event -> itemCount.set(event.getItemCount()));
        dataCommunicator.setViewportRange(0, 50);

        Assert.assertEquals("Invalid item count reported", 0, itemCount.get());

        fakeClientCommunication();

        Assert.assertEquals("Invalid item count reported", 3, itemCount.get());
    }

    @Test
    public void itemCount_undefinedItemCount() {
        final AtomicInteger itemCount = new AtomicInteger(0);
        dataView.addItemCountChangeListener(
                event -> itemCount.set(event.getItemCount()));
        dataCommunicator.setViewportRange(0, 50);
        dataView.setItemCountUnknown();

        Assert.assertEquals("Invalid item count reported", 0, itemCount.get());

        fakeClientCommunication();

        Assert.assertEquals("Invalid item count reported", 3, itemCount.get());

        dataView.setItemCountEstimate(500);

        // since the size was "locked", there is no estimate
        Assert.assertEquals("Invalid item count reported", 3, itemCount.get());

        fakeClientCommunication();

        Assert.assertEquals("Invalid item count reported", 3, itemCount.get());

        // setting new data provider triggers new size from data provider
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            return Stream.generate(String::new).limit(query.getLimit());
        }, query -> 2), null);

        Assert.assertEquals("Invalid item count reported", 3, itemCount.get());

        fakeClientCommunication();

        Assert.assertEquals("Invalid item count reported", 2, itemCount.get());

        dataView.setItemCountEstimate(300);

        Assert.assertEquals("Invalid item count reported", 2, itemCount.get());

        fakeClientCommunication();

        Assert.assertEquals("Invalid item count reported", 300,
                itemCount.get());
    }

    @Test
    public void getItems_withDefinedSize() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.generate(String::new).limit(99);
        }, query -> 99), null);

        Stream<String> items = dataView.getItems();
        Assert.assertEquals("Invalid amount of items returned", 99,
                items.count());
    }

    @Test
    public void getItems_withUndefinedSize() {
        dataCommunicator.setViewportRange(0, 50);
        AtomicInteger limit = new AtomicInteger(50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.generate(String::new).limit(limit.get());
        }, query -> -1), null);

        final int itemCountEstimate = 66;
        dataCommunicator.setItemCountEstimate(itemCountEstimate);

        fakeClientCommunication();

        Assert.assertEquals(
                itemCountEstimate
                        + dataCommunicator.getItemCountEstimateIncrease(),
                dataCommunicator.getItemCount());

        limit.set(70);
        Stream<String> items = dataView.getItems();
        Assert.assertEquals("Invalid amount of items returned", limit.get(),
                items.count());
    }

    @Test
    public void getItem_correctIndex_itemObtained() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> 3), null);

        fakeClientCommunication();

        Assert.assertEquals("Invalid item on index 1", "bar",
                dataView.getItem(1));
    }

    @Test
    public void getItem_negativeIndex_throws() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage("Index must be non-negative");
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> 3), null);

        fakeClientCommunication();
        dataView.getItem(-1);
    }

    @Test
    public void getItem_emptyData_throws() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage("Requested index 0 on empty data.");
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.empty();
        }, query -> 0), null);

        fakeClientCommunication();
        dataView.getItem(0);
    }

    @Test
    public void getItem_outsideOfRange_throws() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage(
                "Given index 3 is outside of the accepted range '0 - 2'");
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> 3), null);

        fakeClientCommunication();
        dataView.getItem(3);
    }

    @Test
    public void getItemIndex_withoutItemIndexProvider_throwUnsupportedOperationException() {
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> dataView.getItemIndex("bar"));
    }

    @Test
    public void getItemIndex_itemPresentedInDataSet_indexFound() {
        dataView.setItemIndexProvider(
                (item, query) -> "bar".equals(item) ? 1 : null);
        Assert.assertEquals("Wrong index returned for item", Optional.of(1),
                dataView.getItemIndex("bar"));
    }

    @Test
    public void getItemIndex_itemNotPresentedInDataSet_indexNotFound() {
        dataView.setItemIndexProvider(
                (item, query) -> "bar".equals(item) ? 1 : null);
        Assert.assertEquals("Wrong index returned for item", Optional.empty(),
                dataView.getItemIndex("notPresent"));
    }

    @Test
    public void refreshItem_itemPresentInDataSet_refreshesItem() {
        Item item1 = new Item(0L, "value1");
        Item item2 = new Item(1L, "value2");

        DataProvider<Item, Void> dataProvider = Mockito
                .spy(DataProvider.fromCallbacks(query -> {
                    query.getOffset();
                    query.getLimit();
                    return Stream.of(item1, item2);
                }, query -> 2));

        DataCommunicator<Item> dataCommunicator = Mockito
                .spy(new DataCommunicator<>((item, jsonObject) -> {
                }, arrayUpdater, null, component.getElement().getNode()));

        dataCommunicator.setDataProvider(dataProvider, null);

        AbstractLazyDataView<Item> dataView = new AbstractLazyDataView<Item>(
                dataCommunicator, component) {
        };

        item1.setValue("updatedValue1");
        dataView.refreshItem(item1);

        // Verify that the refresh request causes both data provider and
        // data communicator invocation.
        Mockito.verify(dataProvider).refreshItem(item1);
        Mockito.verify(dataCommunicator).refresh(item1);

        dataView.setIdentifierProvider(Item::getId);

        Item updatedItem2 = new Item(1L, "updatedValue2");
        dataView.refreshItem(updatedItem2);
        // Verify that the refresh is made on a new object, no on an old
        // object.
        Mockito.verify(dataProvider, Mockito.times(0)).refreshItem(item2);
        Mockito.verify(dataCommunicator, Mockito.times(0)).refresh(item2);
        Mockito.verify(dataProvider).refreshItem(updatedItem2);
        Mockito.verify(dataCommunicator).refresh(updatedItem2);
    }

    @Test
    public void refreshItem_itemNotPresent_itemNotRefreshed() {
        Item item1 = new Item(0L, "value1");

        DataProvider<Item, Void> dataProvider = Mockito
                .spy(DataProvider.fromCallbacks(query -> {
                    query.getOffset();
                    query.getLimit();
                    return Stream.of(item1);
                }, query -> 1));

        DataCommunicator<Item> dataCommunicator = Mockito
                .spy(new DataCommunicator<>((item, jsonObject) -> {
                }, arrayUpdater, null, component.getElement().getNode()));

        dataCommunicator.setDataProvider(dataProvider, null);

        AbstractLazyDataView<Item> dataView = new AbstractLazyDataView<Item>(
                dataCommunicator, component) {
        };

        Item item2 = new Item(1L, "value1");
        dataView.refreshItem(item1);

        // Verify that the refresh request is not promoted to data provider and
        // data communicator, because item with id=1 is not found.
        Mockito.verify(dataProvider, Mockito.times(0)).refreshItem(item2);
        Mockito.verify(dataCommunicator, Mockito.times(0)).refresh(item2);
    }

    @Test
    public void paged_access_methods_in_query_object() {
        Query<Item, Void> query;

        query = new Query<>(0, 20, null, null, null);
        Assert.assertEquals(0L, query.getPage());
        Assert.assertEquals(20, query.getPageSize());

        query = new Query<>(20, 20, null, null, null);
        Assert.assertEquals(1L, query.getPage());
        Assert.assertEquals(20, query.getPageSize());

        query = new Query<>(200, 40, null, null, null);
        Assert.assertEquals(5L, query.getPage());
        Assert.assertEquals(40, query.getPageSize());

    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }
}
