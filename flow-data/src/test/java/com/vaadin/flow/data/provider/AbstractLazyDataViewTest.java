/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.tests.data.bean.Item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractLazyDataViewTest {

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

    @BeforeEach
    void setup() {
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
    void defaults_withCorrectDataProvider_noErrors() {
        dataCommunicator.setDataProvider(dataProvider, null);
        assertTrue(dataView.getDataCommunicator().isDefinedSize());
        assertEquals(BackEndDataProvider.class,
                dataView.getSupportedDataProviderType());
        assertEquals(3, dataView.getDataCommunicator().getItemCount());
        assertEquals(200, dataView.getItemCountEstimate());
        assertEquals(200, dataView.getItemCountEstimateIncrease());

        dataView.setItemCountUnknown();
        assertFalse(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountFromDataProvider();
        assertTrue(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountEstimate(500);
        assertFalse(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountFromDataProvider();
        assertTrue(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountEstimateIncrease(200);
        assertFalse(dataView.getDataCommunicator().isDefinedSize());
    }

    @Test
    void dataViewCreated_beforeSettingDataProvider_verificationPassed() {
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
    void addItemCountListener_beforeSettingDataProvider_verificationPassed() {
        AbstractLazyDataView<String> dataView = new AbstractLazyDataView<String>(
                new DataCommunicator<>((item, jsonObject) -> {
                }, null, null, component.getElement().getNode()), component) {
        };

        // Check that we can add a listener even if no data provider set by
        // user
        dataView.addItemCountChangeListener(event -> {
        });
    }

    @Test
    void dataViewAPIUsed_beforeSettingDataProvider_throws() {
        assertThrows(IllegalStateException.class, () -> {
            AbstractLazyDataView<String> dataView = new AbstractLazyDataView<String>(
                    new DataCommunicator<>((item, jsonObject) -> {
                    }, null, null, component.getElement().getNode()),
                    component) {
            };

            // Check that the verification not passed for any 'lazy load'
            // specific method
            dataView.setItemCountUnknown();
        });
    }

    @Test
    void existingDataView_dataProviderIsChangedToInMemory_throws() {
        assertThrows(IllegalStateException.class, () -> {
            dataCommunicator.setDataProvider(badProvider, null);
            // any method call should be enough to trigger the check for type
            dataView.setItemCountUnknown();
        });
    }

    @Test
    void itemCount_definedItemCount() {
        final AtomicInteger itemCount = new AtomicInteger(0);
        dataView.addItemCountChangeListener(
                event -> itemCount.set(event.getItemCount()));
        dataCommunicator.setViewportRange(0, 50);

        assertEquals(0, itemCount.get(), "Invalid item count reported");

        fakeClientCommunication();

        assertEquals(3, itemCount.get(), "Invalid item count reported");
    }

    @Test
    void itemCount_undefinedItemCount() {
        final AtomicInteger itemCount = new AtomicInteger(0);
        dataView.addItemCountChangeListener(
                event -> itemCount.set(event.getItemCount()));
        dataCommunicator.setViewportRange(0, 50);
        dataView.setItemCountUnknown();

        assertEquals(0, itemCount.get(), "Invalid item count reported");

        fakeClientCommunication();

        assertEquals(3, itemCount.get(), "Invalid item count reported");

        dataView.setItemCountEstimate(500);

        // since the size was "locked", there is no estimate
        assertEquals(3, itemCount.get(), "Invalid item count reported");

        fakeClientCommunication();

        assertEquals(3, itemCount.get(), "Invalid item count reported");

        // setting new data provider triggers new size from data provider
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            return Stream.generate(String::new).limit(query.getLimit());
        }, query -> 2), null);

        assertEquals(3, itemCount.get(), "Invalid item count reported");

        fakeClientCommunication();

        assertEquals(2, itemCount.get(), "Invalid item count reported");

        dataView.setItemCountEstimate(300);

        assertEquals(2, itemCount.get(), "Invalid item count reported");

        fakeClientCommunication();

        assertEquals(300, itemCount.get(), "Invalid item count reported");
    }

    @Test
    void getItems_withDefinedSize() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.generate(String::new).limit(99);
        }, query -> 99), null);

        Stream<String> items = dataView.getItems();
        assertEquals(99, items.count(), "Invalid amount of items returned");
    }

    @Test
    void getItems_withUndefinedSize() {
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

        assertEquals(
                itemCountEstimate
                        + dataCommunicator.getItemCountEstimateIncrease(),
                dataCommunicator.getItemCount());

        limit.set(70);
        Stream<String> items = dataView.getItems();
        assertEquals(limit.get(), items.count(),
                "Invalid amount of items returned");
    }

    @Test
    void getItem_correctIndex_itemObtained() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> 3), null);

        fakeClientCommunication();

        assertEquals("bar", dataView.getItem(1), "Invalid item on index 1");
    }

    @Test
    void getItem_negativeIndex_throws() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> 3), null);

        fakeClientCommunication();
        var ex = assertThrows(IndexOutOfBoundsException.class,
                () -> dataView.getItem(-1));
        assertTrue(ex.getMessage().contains("Index must be non-negative"));
    }

    @Test
    void getItem_emptyData_throws() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.empty();
        }, query -> 0), null);

        fakeClientCommunication();
        var ex = assertThrows(IndexOutOfBoundsException.class,
                () -> dataView.getItem(0));
        assertTrue(
                ex.getMessage().contains("Requested index 0 on empty data."));
    }

    @Test
    void getItem_outsideOfRange_throws() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> 3), null);

        fakeClientCommunication();
        var ex = assertThrows(IndexOutOfBoundsException.class,
                () -> dataView.getItem(3));
        assertTrue(ex.getMessage().contains(
                "Given index 3 is outside of the accepted range '0 - 2'"));
    }

    @Test
    void getItemIndex_withoutItemIndexProvider_throwUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class,
                () -> dataView.getItemIndex("bar"));
    }

    @Test
    void getItemIndex_itemPresentedInDataSet_indexFound() {
        dataView.setItemIndexProvider(
                (item, query) -> "bar".equals(item) ? 1 : null);
        assertEquals(Optional.of(1), dataView.getItemIndex("bar"),
                "Wrong index returned for item");
    }

    @Test
    void getItemIndex_itemNotPresentedInDataSet_indexNotFound() {
        dataView.setItemIndexProvider(
                (item, query) -> "bar".equals(item) ? 1 : null);
        assertEquals(Optional.empty(), dataView.getItemIndex("notPresent"),
                "Wrong index returned for item");
    }

    @Test
    void refreshItem_itemPresentInDataSet_refreshesItem() {
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
    void refreshItem_itemNotPresent_itemNotRefreshed() {
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
    void paged_access_methods_in_query_object() {
        Query<Item, Void> query;

        query = new Query<>(0, 20, null, null, null);
        assertEquals(0L, query.getPage());
        assertEquals(20, query.getPageSize());

        query = new Query<>(20, 20, null, null, null);
        assertEquals(1L, query.getPage());
        assertEquals(20, query.getPageSize());

        query = new Query<>(200, 40, null, null, null);
        assertEquals(5L, query.getPage());
        assertEquals(40, query.getPageSize());

    }

    @Test
    void getItems_withOffsetAndLimit_correctQuery() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            assertEquals(10, query.getOffset(), "Invalid offset");
            assertEquals(20, query.getLimit(), "Invalid limit");
            return Stream.of("foo", "bar");
        }, query -> 100), null);

        Stream<String> items = dataView.getItems(10, 20);
        assertEquals(2, items.count());
    }

    @Test
    void getItems_withOffsetAndLimit_definedSize_limitAdjusted() {
        dataCommunicator.setViewportRange(0, 50);
        // Total items: 100. Offset: 90. Limit: 20.
        // Expect query limit: 10 (100-90).
        AtomicInteger fetchCount = new AtomicInteger(0);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            if (query.getOffset() == 90) {
                assertEquals(10, query.getLimit(), "Invalid limit");
                fetchCount.incrementAndGet();
                return Stream.of("foo");
            }
            query.getLimit();
            query.getOffset();
            return Stream.generate(() -> "dummy").limit(query.getLimit());
        }, query -> 100), null);

        fakeClientCommunication(); // To settle size

        Stream<String> items = dataView.getItems(90, 20);
        assertEquals(1, items.count());
        assertEquals(1, fetchCount.get());
    }

    @Test
    void getItems_withOffsetAndLimit_outOfBounds_emptyStream() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            if (query.getOffset() >= 100) {
                return Stream.empty();
            }
            return Stream.generate(() -> "dummy").limit(query.getLimit());
        }, query -> 100), null);

        fakeClientCommunication();

        Stream<String> items = dataView.getItems(110, 20);
        assertEquals(0, items.count());
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }

    @Test
    void getItems_withNegativeOffset_throwsException() {
        IndexOutOfBoundsException exception = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataView.getItems(-1, 10));
        assertEquals("Offset must be non-negative", exception.getMessage());
    }

    @Test
    void getItems_withNegativeLimit_throwsException() {
        IndexOutOfBoundsException exception = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataView.getItems(0, -1));
        assertEquals("Limit must be non-negative", exception.getMessage());
    }
}
