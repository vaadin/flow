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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class DataCommunicatorTest {
    /**
     * Test item that uses id for identity.
     */
    private static class Item {
        private final int id;
        private String value;

        public Item(int id) {
            this(id, "Item " + id);
        }

        public Item(int id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public String toString() {
            return id + ": " + value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Item) {
                Item that = (Item) obj;
                return that.id == id;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    private DataCommunicator<Item> dataCommunicator;

    @Mock
    private DataGenerator<Item> dataGenerator;
    @Mock
    private ArrayUpdater arrayUpdater;

    private Element element;
    private MockUI ui;

    private ArrayUpdater.Update update;

    public Range lastClear = null;
    public Range lastSet = null;
    public int lastUpdateId = -1;
    private int pageSize;

    abstract boolean isDataProviderWithParallelStream();

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        ui = new MockUI();
        element = new Element("div");
        ui.getElement().appendChild(element);

        lastClear = null;
        lastSet = null;
        lastUpdateId = -1;

        update = new ArrayUpdater.Update() {

            @Override
            public void clear(int start, int length) {
                lastClear = Range.withLength(start, length);
            }

            @Override
            public void set(int start, List<JsonNode> items) {
                lastSet = Range.withLength(start, items.size());
            }

            @Override
            public void commit(int updateId) {
                lastUpdateId = updateId;
            }
        };

        Mockito.when(arrayUpdater.startUpdate(Mockito.anyInt()))
                .thenReturn(update);

        dataCommunicator = new DataCommunicator<>(dataGenerator, arrayUpdater,
                data -> {
                }, element.getNode());
        pageSize = dataCommunicator.getPageSize();
    }

    @Test
    public void communicator_with_0_items_should_not_refresh_all() {
        dataCommunicator.setViewportRange(0, 0);
        fakeClientCommunication();

        Assertions.assertEquals(Range.withLength(0, 0), lastSet);
        Assertions.assertNull(lastClear,
                "Only requestAll should clear items. This may make us loop.");

        dataCommunicator.setViewportRange(0, 0);
        fakeClientCommunication();

        Assertions.assertEquals(Range.withLength(0, 0), lastSet);
        Assertions.assertNull(lastClear,
                "Only requestAll should clear items. Which would make us loop.");
    }

    @Test
    public void communicator_with_items_should_send_updates_but_not_refresh_all() {
        dataCommunicator.setDataProvider(createDataProvider(), null);

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertEquals(Range.withLength(0, 50), lastSet,
                "Expected request range for 50 items on first request.");

        dataCommunicator.setViewportRange(0, 70);
        fakeClientCommunication();

        Assertions.assertEquals(Range.withLength(50, 20), lastSet,
                "Expected request range for 20 new items.");
    }

    @Test
    public void reattach_different_roundtrip_refresh_all() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertEquals(Range.withLength(0, 50), lastSet,
                "Expected initial full reset.");
        lastSet = null;

        element.removeFromParent();
        fakeClientCommunication();

        Assertions.assertNull(lastSet, "Expected no during reattach.");

        ui.getElement().appendChild(element);
        fakeClientCommunication();

        Assertions.assertEquals(Range.withLength(0, 50), lastSet,
                "Expected initial full reset after reattach");
    }

    @Test
    public void reattach_same_roundtrip_refresh_nothing() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertEquals(Range.withLength(0, 50), lastSet,
                "Expected initial full reset.");
        lastSet = null;

        element.removeFromParent();

        Assertions.assertNull(lastSet,
                "Expected no communication during reattach");

        ui.getElement().appendChild(element);
        fakeClientCommunication();

        Assertions.assertNull(lastSet,
                "Expected no communication after reattach");
    }

    @Test
    public void refreshViewport_updatedRangeSent() {
        var compositeDataGenerator = new CompositeDataGenerator<Item>();
        dataCommunicator = new DataCommunicator<>(compositeDataGenerator,
                arrayUpdater, data -> {
                }, element.getNode()) {
        };
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setViewportRange(0, 6);

        var count = new AtomicInteger(0);
        compositeDataGenerator.addDataGenerator(new DataGenerator<Item>() {
            @Override
            public void generateData(Item item, ObjectNode json) {
                json.put("count", String.valueOf(count.get()));
            }
        });

        fakeClientCommunication();
        Assertions.assertEquals(Range.withLength(0, 6), lastSet);
        lastSet = null;

        dataCommunicator.refreshViewport();
        fakeClientCommunication();
        Assertions.assertEquals(Range.withLength(0, 6), lastSet);
    }

    @Test
    public void setFlushRequest_remove_setFlushRequest_reattach_noEndlessFlushLoop() {
        AtomicInteger listenerInvocationCounter = new AtomicInteger(0);
        dataCommunicator = new DataCommunicator<>(dataGenerator, arrayUpdater,
                data -> {
                }, element.getNode()) {
            @Override
            public void reset() {
                Assertions.assertTrue(
                        listenerInvocationCounter.incrementAndGet() < 5);
                super.reset();
            }
        };

        // No flush requests initially
        fakeClientCommunication();

        dataCommunicator.reset();
        element.removeFromTree();
        dataCommunicator.reset();
        ui.getElement().appendChild(element);

        listenerInvocationCounter.set(0);

        fakeClientCommunication();
    }

    @Test
    public void setDataProvider_keyMapperIsReset() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertEquals(0, dataCommunicator.getKeyMapper().get("1").id);

        dataCommunicator.setDataProvider(createDataProvider(), null);
        Assertions.assertNull(dataCommunicator.getKeyMapper().get("1"),
                "The KeyMapper should be reset when a new DataProvider is set");
    }

    @Test
    public void dataProviderBreaksContract_limitOrPageSizeAreNotCalled_throw() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            items.add(new Item(i));
        }
        DataProvider<Item, Void> dataProvider = DataProvider
                .fromCallbacks(query -> items.stream(), query -> items.size());
        dataCommunicator.setDataProvider(dataProvider, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> dataCommunicator.fetchFromProvider(0, 1));
        Assertions.assertTrue(ex.getMessage()
                .contains("The data provider hasn't ever called getLimit() or "
                        + "getPageSize()"));
    }

    @Test
    public void dataProviderBreaksContract_offsetOrPageAreNotCalled_throw() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            items.add(new Item(i));
        }
        DataProvider<Item, Void> dataProvider = DataProvider
                .fromCallbacks(query -> {
                    query.getLimit();
                    return items.stream();
                }, query -> items.size());
        dataCommunicator.setDataProvider(dataProvider, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> dataCommunicator.fetchFromProvider(1, 1));
        Assertions.assertTrue(ex.getMessage().contains(
                "The data provider hasn't ever called getOffset() or getPage()"));
    }

    @Test
    public void dataProviderContract_pageAndPageSizeAreCalled_itemsFetched() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            items.add(new Item(i));
        }
        DataProvider<Item, Void> dataProvider = DataProvider
                .fromCallbacks(query -> {
                    query.getPage();
                    query.getPageSize();
                    return items.stream();
                }, query -> items.size());
        dataCommunicator.setDataProvider(dataProvider, null);
        Assertions.assertEquals(2,
                dataCommunicator.fetchFromProvider(1, 1).count());
    }

    @Test
    public void dataProviderBreaksContract_tooManyItems_throw() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            items.add(new Item(i));
        }
        DataProvider<Item, Void> dataProvider = DataProvider
                .fromCallbacks(query -> {
                    query.getOffset();
                    query.getLimit();
                    return items.stream();
                }, query -> items.size());
        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.setPageSize(2);

        // limit is less than two pages (4), but the backed returns more than
        // 4 items, which is incorrect
        Stream<Item> stream = dataCommunicator.fetchFromProvider(0, 3);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> stream.forEach(item -> {
                }));
        Assertions.assertTrue(ex.getMessage()
                .contains("exceeds the limit specified by the query (4)."));
    }

    @Test
    public void sameKeyDifferentInstance_latestInstanceUsed() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            items.add(new Item(i));
        }
        // Abusing the fact that ListDataProvider doesn't copy the backing store
        ListDataProvider<Item> dataProvider = new ListDataProvider<>(items);
        dataCommunicator.setDataProvider(dataProvider, null);

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Item originalItem = items.get(0);
        String key = dataCommunicator.getKeyMapper().key(originalItem);

        Assertions.assertSame(originalItem,
                dataCommunicator.getKeyMapper().get(key));

        Item updatedItem = new Item(originalItem.id, "Updated");
        items.set(0, updatedItem);
        dataProvider.refreshAll();

        fakeClientCommunication();

        Assertions.assertSame(updatedItem,
                dataCommunicator.getKeyMapper().get(key));
    }

    @Test
    public void dataProviderReturnsLessItemsThanRequested_aNewSizeQueryIsPerformed() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProviderThatChangesSize(
                50, 10);
        dataProvider = Mockito.spy(dataProvider);
        dataCommunicator.setDataProvider(dataProvider, null);

        // The first request will return size 50, but the actual fetch will
        // bring only 40 items. A new size query should then be performed, that
        // will return 40 instead
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertEquals(40, lastSet.getEnd());
        // Assert takes into acount the inital size query for setting
        // dataprovider.
        Mockito.verify(dataProvider, Mockito.times(2)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());
    }

    @Test
    public void setSizeCallback_usedForDataSize() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider();
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.setViewportRange(0, 50);
        Assertions.assertTrue(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        AtomicBoolean sizeCallbackCall = new AtomicBoolean(false);
        dataCommunicator.setCountCallback(query -> {
            sizeCallbackCall.set(true);
            return 100;
        });
        Assertions.assertTrue(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        Assertions.assertTrue(sizeCallbackCall.getAndSet(false),
                "SizeCallback not called");
        Assertions.assertEquals(100, dataCommunicator.getItemCount(),
                "Size not used");

        Mockito.verify(dataProvider, Mockito.times(1)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());

        dataCommunicator.setViewportRange(50, 50);

        fakeClientCommunication();

        Mockito.verify(dataProvider, Mockito.times(1)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(2)).fetch(Mockito.any());

        Assertions.assertFalse(sizeCallbackCall.get(),
                "SizeCallback called when should not have");
    }

    @Test
    public void setSizeCallback_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            dataCommunicator.setDataProvider(createDataProvider(), null);
            dataCommunicator.setCountCallback(null);
        });
    }

    @Test
    public void setCountCallback_itemCountEstimatesWereSet_overridesItemCountEstimates() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                5000);
        dataProvider = Mockito.spy(dataProvider);
        dataCommunicator.setDataProvider(dataProvider, null);

        final int itemCountEstimate = 200;
        dataCommunicator.setItemCountEstimate(itemCountEstimate);
        final int itemCountEstimateIncrease = 300;
        dataCommunicator
                .setItemCountEstimateIncrease(itemCountEstimateIncrease);
        dataCommunicator.setViewportRange(150, 50);
        Assertions.assertFalse(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        Assertions.assertEquals(itemCountEstimate + itemCountEstimateIncrease,
                dataCommunicator.getItemCount(),
                "initial estimate+increase not used");
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());

        AtomicBoolean sizeCallbackCall = new AtomicBoolean(false);
        final int exactSize = 1234;
        dataCommunicator.setCountCallback(query -> {
            sizeCallbackCall.set(true);
            return exactSize;
        });
        Assertions.assertTrue(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        Assertions.assertTrue(sizeCallbackCall.getAndSet(false),
                "SizeCallback not called");
        Assertions.assertEquals(exactSize, dataCommunicator.getItemCount(),
                "Size not used");
    }

    @Test
    public void setInitialCountEstimate_usedInitiallyThenDiscarded() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                250);
        dataProvider = Mockito.spy(dataProvider);
        dataCommunicator.setDataProvider(dataProvider, null);

        final int initialCountEstimate = 100;
        dataCommunicator.setItemCountEstimate(initialCountEstimate);
        dataCommunicator.setViewportRange(0, 50);
        Assertions.assertFalse(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        Assertions.assertEquals(initialCountEstimate,
                dataCommunicator.getItemCount(),
                "initial size estimate not used");
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());

        dataCommunicator.setViewportRange(50, 50);

        fakeClientCommunication();

        Assertions.assertEquals(initialCountEstimate + getPageSizeIncrease(),
                dataCommunicator.getItemCount(),
                "initial size estimate was not discarded");
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(2)).fetch(Mockito.any());
    }

    @Test
    public void setInitialCountEstimate_lessThanCurrentFetchedSize_discarded() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                250);

        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.setDefinedSize(false);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        final int initialCountEstimate = 111;
        dataCommunicator.setItemCountEstimate(initialCountEstimate);
        Assertions.assertFalse(dataCommunicator.isDefinedSize());

        dataCommunicator.setViewportRange(50, 100);
        fakeClientCommunication();

        Assertions.assertEquals(initialCountEstimate + getPageSizeIncrease(),
                dataCommunicator.getItemCount(),
                "too small initial size estimate should not be applied");
    }

    @Test
    public void setInitialCountEstimate_lessThanRequestedRange_sizeIsIncreasedAutomatically() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                250);
        dataProvider = Mockito.spy(dataProvider);
        dataCommunicator.setDataProvider(dataProvider, null);
        int requestedRangeEnd = 50;
        dataCommunicator.setViewportRange(0, requestedRangeEnd);

        final int initialCountEstimate = 49;
        dataCommunicator.setItemCountEstimate(initialCountEstimate);

        fakeClientCommunication();
        Assertions.assertEquals(initialCountEstimate + getPageSizeIncrease(),
                dataCommunicator.getItemCount(),
                "Size should be automatically adjusted for too small estimate");
    }

    @Test
    public void setInitialItemCountEstimateAndIncrease_lessThanRequestedRange_estimateIncreaseUsed() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                5000);
        dataProvider = Mockito.spy(dataProvider);
        dataCommunicator.setDataProvider(dataProvider, null);
        int rangeLength = 100;
        dataCommunicator.setViewportRange(400, rangeLength);

        final int initialCountEstimate = 300;
        dataCommunicator.setItemCountEstimate(initialCountEstimate);
        final int itemCountEstimateIncrease = 99;
        dataCommunicator
                .setItemCountEstimateIncrease(itemCountEstimateIncrease);

        fakeClientCommunication();
        Assertions.assertEquals(
                initialCountEstimate + (3 * itemCountEstimateIncrease),
                dataCommunicator.getItemCount(),
                "Size should be automatically adjusted for too small estimate");
    }

    @Test
    public void setInitialItemCountEstimateAndIncrease_requestedItemsMuchHigherThanExactCount_exactCountIsResolvedOnServer() {
        final int exactSize = 200;
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                exactSize);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        final int itemCountEstimate = 1000;
        dataCommunicator.setItemCountEstimate(itemCountEstimate);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertEquals(itemCountEstimate,
                dataCommunicator.getItemCount());

        // if the user scrolls far from the exact size of the backend,
        // the exact size is resolved on the server side without causing a new
        // roundtrip where the client will request items because it received
        // less
        // items than expected
        dataCommunicator.setViewportRange(900, 100);
        fakeClientCommunication();

        Assertions.assertEquals(exactSize, dataCommunicator.getItemCount());
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        // 1. initial call 0-50, 2. then: 900-950, 800-850, ... ,
        // 200-250, 100-150, 150-200.
        // Ranges 950-1000, 850-900, ... , 250-300 are not requested
        // from backend because it's clear they would be empty (because
        // 900-950, ... 200-250 are empty).
        // So, it would be 11 calls in total:
        Mockito.verify(dataProvider, Mockito.times(11)).fetch(Mockito.any());
    }

    @Test
    public void setInitialItemCountEstimateAndIncrease_backendEmpty_noEndlessFlushLoop() {
        final int exactSize = 0;
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                exactSize);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        final int itemCountEstimate = 1000;
        dataCommunicator.setItemCountEstimate(itemCountEstimate);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertEquals(exactSize, dataCommunicator.getItemCount());
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());
    }

    @Test
    public void setInitialCountEstimate_lessThanOne_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            dataCommunicator.setItemCountEstimate(0);
        });
    }

    @Test
    public void getActiveItemOnIndex_activeRangeChanges_itemsReturned() {
        dataCommunicator.setDataProvider(createDataProvider(300), null);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();
        Assertions.assertEquals(new Item(0), dataCommunicator.getItem(0),
                "Wrong active item");
        Assertions.assertEquals(new Item(49), dataCommunicator.getItem(49),
                "Wrong active item");

        dataCommunicator.setViewportRange(50, 50);
        fakeClientCommunication();

        Assertions.assertEquals(new Item(50), dataCommunicator.getItem(50),
                "Wrong active item");
        Assertions.assertEquals(new Item(69), dataCommunicator.getItem(69),
                "Wrong active item");
        Assertions.assertEquals(new Item(99), dataCommunicator.getItem(99),
                "Wrong active item");
    }

    @Test
    public void isItemActive_newItems() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setViewportRange(0, 50);

        Assertions.assertFalse(dataCommunicator.isItemActive(new Item(0)),
                "Item should not be active");

        fakeClientCommunication();

        Assertions.assertTrue(dataCommunicator.isItemActive(new Item(0)),
                "Item should be active");
        Assertions.assertTrue(dataCommunicator.isItemActive(new Item(49)),
                "Item should be active");
        Assertions.assertFalse(dataCommunicator.isItemActive(new Item(50)),
                "Item should not be active");

        dataCommunicator.setViewportRange(50, 50);
        fakeClientCommunication();

        Assertions.assertTrue(dataCommunicator.isItemActive(new Item(50)),
                "Item should be active");
        Assertions.assertTrue(dataCommunicator.isItemActive(new Item(99)),
                "Item should be active");
        Assertions.assertFalse(dataCommunicator.isItemActive(new Item(100)),
                "Item should not be active");
    }

    @Test
    public void getItem_withDefinedSizeAndCorrectIndex() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return IntStream.of(0, 1, 2).mapToObj(Item::new);
        }, query -> 3), null);

        fakeClientCommunication();

        // Request the item within the active range
        Assertions.assertEquals(new Item(1), dataCommunicator.getItem(1),
                "Invalid item on index 1");

        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(
                query -> IntStream.range(0, 300).mapToObj(Item::new)
                        .skip(query.getOffset()).limit(query.getLimit()),
                query -> 300), null);

        fakeClientCommunication();

        // Request an item outside the active range
        Assertions.assertEquals(new Item(260), dataCommunicator.getItem(260),
                "Invalid item on index 260");
    }

    @Test
    public void getItem_withDefinedSizeAndNegativeIndex() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of(new Item(0));
        }, query -> 1), null);

        fakeClientCommunication();
        IndexOutOfBoundsException ex = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataCommunicator.getItem(-1));
        Assertions.assertTrue(
                ex.getMessage().contains("Index must be non-negative"));
    }

    @Test
    public void getItem_withDefinedSizeAndEmptyDataset() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.empty();
        }, query -> 0), null);

        fakeClientCommunication();
        IndexOutOfBoundsException ex = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataCommunicator.getItem(0));
        Assertions.assertTrue(
                ex.getMessage().contains("Requested index 0 on empty data."));
    }

    @Test
    public void getItem_withDefinedSizeAndIndexOutsideOfRange() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return IntStream.of(0, 1, 2).mapToObj(Item::new);
        }, query -> 3), null);

        fakeClientCommunication();
        IndexOutOfBoundsException ex = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataCommunicator.getItem(3));
        Assertions.assertTrue(ex.getMessage().contains(
                "Given index 3 is outside of the accepted range '0 - 2'"));
    }

    @Test
    public void getItem_withDefinedSizeAndFiltering() {
        final Item initialFilter = new Item(1); // filters all except 2nd item
        final Item newFilter = new Item(2); // filters all except 3rd item

        dataCommunicator.setViewportRange(0, 50);
        SerializableConsumer<Item> newFilterProvider = dataCommunicator
                .setDataProvider(DataProvider.fromFilteringCallbacks(query -> {
                    query.getOffset();
                    query.getLimit();
                    return IntStream.of(0, 1, 2).mapToObj(Item::new).filter(
                            item -> item.equals(query.getFilter().get()));
                }, query -> 1), initialFilter);

        fakeClientCommunication();
        Assertions.assertEquals(initialFilter, dataCommunicator.getItem(0),
                "Invalid item on index 0");

        newFilterProvider.accept(newFilter);

        fakeClientCommunication();
        Assertions.assertEquals(newFilter, dataCommunicator.getItem(0),
                "Invalid item on index 0");
    }

    @Test
    public void getItem_withDefinedSizeAndSorting() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            Stream<Item> stream = IntStream.of(1, 2, 0).mapToObj(Item::new);
            if (query.getInMemorySorting() != null) {
                stream = stream.sorted(query.getInMemorySorting());
            }
            return stream;
        }, query -> 3), null);

        fakeClientCommunication();
        Assertions.assertEquals(new Item(1), dataCommunicator.getItem(0),
                "Invalid item on index 0");
        Assertions.assertEquals(new Item(2), dataCommunicator.getItem(1),
                "Invalid item on index 1");
        Assertions.assertEquals(new Item(0), dataCommunicator.getItem(2),
                "Invalid item on index 2");

        dataCommunicator
                .setInMemorySorting((i1, i2) -> Integer.compare(i1.id, i2.id));

        fakeClientCommunication();
        Assertions.assertEquals(new Item(0), dataCommunicator.getItem(0),
                "Invalid item on index 0");
        Assertions.assertEquals(new Item(1), dataCommunicator.getItem(1),
                "Invalid item on index 1");
        Assertions.assertEquals(new Item(2), dataCommunicator.getItem(2),
                "Invalid item on index 2");
    }

    @Test
    public void getItem_withUndefinedSizeAndCorrectIndex() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(
                query -> IntStream.of(0, 1, 2).mapToObj(Item::new)
                        .skip(query.getOffset()).limit(query.getLimit()),
                query -> -1), null);

        dataCommunicator.setItemCountEstimate(5);
        fakeClientCommunication();

        // Request the item within the active range
        Assertions.assertEquals(new Item(0), dataCommunicator.getItem(0),
                "Wrong item on index 0");
        Assertions.assertEquals(new Item(1), dataCommunicator.getItem(1),
                "Wrong item on index 1");

        dataCommunicator.setViewportRange(100, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(
                query -> IntStream.range(0, 500).mapToObj(Item::new)
                        .skip(query.getOffset()).limit(query.getLimit()),
                query -> -1), null);

        final int itemCountEstimate = 400;
        dataCommunicator.setItemCountEstimate(itemCountEstimate);

        fakeClientCommunication();

        // Request the item outside the active range, but within the
        // estimation (and present in the backend)
        Assertions.assertEquals(new Item(375), dataCommunicator.getItem(375),
                "Wrong item on index 375");

        // Request the item outside the active range, and outside the
        // estimation (and present in the backend)
        Assertions.assertEquals(new Item(450), dataCommunicator.getItem(450),
                "Wrong item on index 450");
    }

    @Test
    public void getItem_withUndefinedSizeAndEmptyDataset() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(
                query -> IntStream.of(0, 1, 2).mapToObj(Item::new)
                        .skip(query.getOffset()).limit(query.getLimit()),
                query -> -1), null);

        dataCommunicator.setItemCountEstimate(5);
        // This checks the situation when the fetch actions has not happened
        // yet but the data set contains the requested item
        Assertions.assertEquals(new Item(1), dataCommunicator.getItem(1),
                "Invalid item on index 1");

        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.empty();
        }, query -> -1), null);

        dataCommunicator.setItemCountEstimate(2);
        fakeClientCommunication();
        // This checks the situation when the fetch actions has happened
        // but the data set is empty
        Assertions.assertNull(dataCommunicator.getItem(0),
                "Item on index 0 supposed to be null");
    }

    @Test
    public void getItem_withUndefinedSizeAndIndexOutsideOfRange() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(
                query -> IntStream.of(0, 1, 2, 3, 4).mapToObj(Item::new)
                        .skip(query.getOffset()).limit(query.getLimit()),
                query -> -1), null);

        dataCommunicator.setItemCountEstimate(3);
        fakeClientCommunication();

        // Index 3 is outside of estimation but the requested item is in
        // backend anyway
        Assertions.assertEquals(new Item(3), dataCommunicator.getItem(3),
                "Invalid item on index 3");

        // Index 5 is outside of estimation and not in the backend
        Assertions.assertNull(dataCommunicator.getItem(5),
                "Item on index 5 supposed to be null");
    }

    @Test
    public void getItem_withUndefinedSizeAndNegativeIndex() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of(new Item(0));
        }, query -> -1), null);

        dataCommunicator.setItemCountEstimate(1);
        fakeClientCommunication();
        IndexOutOfBoundsException ex = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataCommunicator.getItem(-1));
        Assertions.assertTrue(
                ex.getMessage().contains("Index must be non-negative"));
    }

    @Test
    public void getItem_withUndefinedSizeAndFiltering() {
        final Item initialFilter = new Item(1); // filters all except 2nd item
        final Item newFilter = new Item(2); // filters all except 3rd item

        dataCommunicator.setViewportRange(0, 50);
        SerializableConsumer<Item> newFilterProvider = dataCommunicator
                .setDataProvider(DataProvider.fromFilteringCallbacks(query -> {
                    query.getOffset();
                    query.getLimit();
                    return IntStream.of(0, 1, 2).mapToObj(Item::new).filter(
                            item -> item.equals(query.getFilter().get()));
                }, query -> -1), initialFilter);

        dataCommunicator.setItemCountEstimate(5);

        fakeClientCommunication();
        Assertions.assertEquals(initialFilter, dataCommunicator.getItem(0),
                "Invalid item on index 0");

        newFilterProvider.accept(newFilter);

        fakeClientCommunication();
        Assertions.assertEquals(newFilter, dataCommunicator.getItem(0),
                "Invalid item on index 0");
    }

    @Test
    public void getItem_withUndefinedSizeAndSorting() {
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            Stream<Item> stream = IntStream.of(1, 2, 0).mapToObj(Item::new);
            if (query.getInMemorySorting() != null) {
                stream = stream.sorted(query.getInMemorySorting());
            }
            return stream;
        }, query -> 3), null);

        dataCommunicator.setItemCountEstimate(5);

        fakeClientCommunication();
        Assertions.assertEquals(new Item(1), dataCommunicator.getItem(0),
                "Invalid item on index 0");
        Assertions.assertEquals(new Item(2), dataCommunicator.getItem(1),
                "Invalid item on index 1");
        Assertions.assertEquals(new Item(0), dataCommunicator.getItem(2),
                "Invalid item on index 2");

        dataCommunicator
                .setInMemorySorting((i1, i2) -> Integer.compare(i1.id, i2.id));

        fakeClientCommunication();
        Assertions.assertEquals(new Item(0), dataCommunicator.getItem(0),
                "Invalid item on index 0");
        Assertions.assertEquals(new Item(1), dataCommunicator.getItem(1),
                "Invalid item on index 1");
        Assertions.assertEquals(new Item(2), dataCommunicator.getItem(2),
                "Invalid item on index 2");
    }

    @Test
    public void getItem_streamIsClosed() {
        AtomicBoolean streamIsClosed = new AtomicBoolean();
        dataCommunicator.setDataProvider(createDataProvider(streamIsClosed),
                null);

        fakeClientCommunication();
        dataCommunicator.getItem(0);

        Assertions.assertTrue(streamIsClosed.get());
    }

    @Test
    public void itemCountEstimateAndStep_defaults() {
        Assertions.assertEquals(dataCommunicator.getItemCountEstimate(),
                pageSize * 4);
        Assertions.assertEquals(dataCommunicator.getItemCountEstimateIncrease(),
                pageSize * 4);

        int customPageSize = 100;
        dataCommunicator.setPageSize(customPageSize);

        Assertions.assertEquals(dataCommunicator.getItemCountEstimate(),
                customPageSize * 4);
        Assertions.assertEquals(dataCommunicator.getItemCountEstimateIncrease(),
                customPageSize * 4);

        int customItemCountEstimate = 123;
        dataCommunicator.setItemCountEstimate(customItemCountEstimate);
        int customItemCountEstimateStep = 456;
        dataCommunicator
                .setItemCountEstimateIncrease(customItemCountEstimateStep);

        Assertions.assertEquals(dataCommunicator.getItemCountEstimate(),
                customItemCountEstimate);
        Assertions.assertEquals(dataCommunicator.getItemCountEstimateIncrease(),
                customItemCountEstimateStep);
    }

    @Test
    public void itemCountChangeEvent_exactSize_correctCountAndIsCountEstimated() {
        final TestComponent component = new TestComponent();
        ui.add(component);
        dataCommunicator = new DataCommunicator<>(dataGenerator, arrayUpdater,
                data -> {
                }, component.getElement().getNode());
        AtomicReference<ItemCountChangeEvent<?>> cachedEvent = new AtomicReference<>();
        ComponentUtil.addListener(component, ItemCountChangeEvent.class,
                ((ComponentEventListener) event -> {
                    Assertions.assertNull(cachedEvent.get());
                    cachedEvent.set((ItemCountChangeEvent<?>) event);
                }));
        int exactCount = 500;
        dataCommunicator.setDataProvider(createDataProvider(exactCount), null);
        dataCommunicator.setViewportRange(0, 50);
        // exact size
        fakeClientCommunication();

        ItemCountChangeEvent<?> event = cachedEvent.getAndSet(null);

        Assertions.assertEquals(exactCount, event.getItemCount(),
                "Invalid count provided");
        Assertions.assertFalse(event.isItemCountEstimated());
        // no new event fired
        dataCommunicator.setViewportRange(450, 50);
        fakeClientCommunication();
        Assertions.assertNull(cachedEvent.get());

        // creating a new data provider with same exact size -> no new event
        // fired
        dataCommunicator.setDataProvider(createDataProvider(500), null);
        fakeClientCommunication();
        Assertions.assertNull(cachedEvent.get());

        // new data provider with different size
        dataCommunicator.setDataProvider(createDataProvider(exactCount = 1000),
                null);
        fakeClientCommunication();
        event = cachedEvent.getAndSet(null);

        Assertions.assertEquals(exactCount, event.getItemCount(),
                "Invalid count provided");
        Assertions.assertFalse(event.isItemCountEstimated());
    }

    @Test
    public void itemCountChangeEvent_estimatedCount_estimateUsedUntilEndReached() {
        final TestComponent component = new TestComponent();
        ui.add(component);
        dataCommunicator = new DataCommunicator<>(dataGenerator, arrayUpdater,
                data -> {
                }, component.getElement().getNode());
        AtomicReference<ItemCountChangeEvent<?>> cachedEvent = new AtomicReference<>();
        ComponentUtil.addListener(component, ItemCountChangeEvent.class,
                ((ComponentEventListener) event -> {
                    Assertions.assertNull(cachedEvent.get());
                    cachedEvent.set((ItemCountChangeEvent<?>) event);
                }));
        int exactCount = 500;
        dataCommunicator.setDataProvider(createDataProvider(exactCount), null);
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDefinedSize(false);
        // initial estimate count of 200
        fakeClientCommunication();

        ItemCountChangeEvent<?> event = cachedEvent.getAndSet(null);
        Assertions.assertEquals(200, event.getItemCount(),
                "Invalid count provided");
        Assertions.assertTrue(event.isItemCountEstimated());

        dataCommunicator.setViewportRange(150, 50);
        fakeClientCommunication();

        event = cachedEvent.getAndSet(null);
        Assertions.assertEquals(400, event.getItemCount(),
                "Invalid count provided");
        Assertions.assertTrue(event.isItemCountEstimated());

        dataCommunicator.setViewportRange(350, 50);
        fakeClientCommunication();

        event = cachedEvent.getAndSet(null);
        Assertions.assertEquals(600, event.getItemCount(),
                "Invalid count provided");
        Assertions.assertTrue(event.isItemCountEstimated());

        dataCommunicator.setViewportRange(550, 50);
        fakeClientCommunication();

        // reaching exact size
        event = cachedEvent.getAndSet(null);
        Assertions.assertEquals(500, event.getItemCount(),
                "Invalid count provided");
        Assertions.assertFalse(event.isItemCountEstimated());
    }

    @Test
    public void setDefinedSize_rangeEndEqualsAssumedSize_flushRequested() {
        // trigger client communication in order to initialise it and avoid
        // infinite loop inside 'requestFlush()'
        fakeClientCommunication();

        StateNode stateNode = Mockito.spy(element.getNode());
        DataCommunicator<Item> dataCommunicator = new DataCommunicator<>(
                dataGenerator, arrayUpdater, data -> {
                }, stateNode);

        // the items size returned by this data provider will be 100
        dataCommunicator.setDataProvider(createDataProvider(), null);

        // Trigger flush() to set the assumedSize
        fakeClientCommunication();

        dataCommunicator.setViewportRange(0, 100);
        // clean flushRequest
        fakeClientCommunication();

        Mockito.reset(stateNode);
        dataCommunicator.setDefinedSize(false);
        fakeClientCommunication();

        // Verify that requestFlush has been invoked
        Mockito.verify(stateNode).runWhenAttached(Mockito.any());

        // Verify the estimated count is now 100 + 4 * pageSize = 300
        Assertions.assertEquals(300, dataCommunicator.getItemCount());
    }

    @Test
    public void pageSize_defaultPageSizeUsed_returnsItemNormally() {
        dataCommunicator.setDataProvider(DataProvider.ofItems(new Item(0)),
                null);
        Stream<Item> itemStream = dataCommunicator.fetchFromProvider(0, 100);
        Assertions.assertNotNull(itemStream);
        Assertions.assertEquals(1, itemStream.count());
    }

    @Test
    public void fetchFromProvider_pageSizeLessThanLimit_multiplePagedQueries() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                100);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setPageSize(10);
        dataCommunicator.setDataProvider(dataProvider, null);
        // Use a limit value so that it's not multiple by page size
        Stream<Item> stream = dataCommunicator.fetchFromProvider(0, 23);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);

        Mockito.verify(dataProvider, Mockito.times(3))
                .fetch(queryCaptor.capture());

        List<Item> items = stream.collect(Collectors.toList());
        Assertions.assertEquals(30, items.size());

        Assertions.assertEquals(IntStream.range(0, 30).mapToObj(Item::new)
                .collect(Collectors.toList()), items);

        List<Query> allQueries = queryCaptor.getAllValues();
        Assertions.assertEquals(3, allQueries.size());

        Query query = allQueries.get(0);
        Assertions.assertEquals(0, query.getOffset());
        Assertions.assertEquals(10, query.getLimit());
        Assertions.assertEquals(0, query.getPage());
        Assertions.assertEquals(10, query.getPageSize());

        query = allQueries.get(1);
        Assertions.assertEquals(10, query.getOffset());
        Assertions.assertEquals(10, query.getLimit());
        Assertions.assertEquals(1, query.getPage());
        Assertions.assertEquals(10, query.getPageSize());

        query = allQueries.get(2);
        Assertions.assertEquals(20, query.getOffset());
        Assertions.assertEquals(10, query.getLimit());
        Assertions.assertEquals(2, query.getPage());
        Assertions.assertEquals(10, query.getPageSize());

        Mockito.reset(dataProvider);
        // Use a limit value so that it's multiple by page size
        dataCommunicator.fetchFromProvider(0, 30);
        Mockito.verify(dataProvider, Mockito.times(3))
                .fetch(Mockito.any(Query.class));
    }

    @Test
    public void fetchFromProvider_limitEqualsPageSize_singleQuery() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                100);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        Stream<Item> stream = dataCommunicator.fetchFromProvider(0, 50);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);

        Mockito.verify(dataProvider).fetch(queryCaptor.capture());

        List<Item> items = stream.collect(Collectors.toList());
        Assertions.assertEquals(50, items.size());

        Assertions.assertEquals(IntStream.range(0, 50).mapToObj(Item::new)
                .collect(Collectors.toList()), items);

        Query query = queryCaptor.getValue();
        Assertions.assertEquals(0, query.getOffset());
        Assertions.assertEquals(50, query.getLimit());
        Assertions.assertEquals(0, query.getPage());
        Assertions.assertEquals(50, query.getPageSize());
    }

    @Test
    public void fetchFromProvider_limitLessThanPageSize_singleQuery_fetchedLessThanPage() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                100);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        Stream<Item> stream = dataCommunicator.fetchFromProvider(10, 42);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);

        Mockito.verify(dataProvider).fetch(queryCaptor.capture());

        List<Item> items = stream.collect(Collectors.toList());
        Assertions.assertEquals(42, items.size());

        Assertions.assertEquals(IntStream.range(10, 52).mapToObj(Item::new)
                .collect(Collectors.toList()), items);

        Query query = queryCaptor.getValue();
        Assertions.assertEquals(10, query.getOffset());
        Assertions.assertEquals(42, query.getLimit());
        Assertions.assertEquals(0, query.getPage());
        Assertions.assertEquals(42, query.getPageSize());
    }

    @Test
    public void fetchFromProvider_disablePaging_singleQueryWithLimit() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                200);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setPagingEnabled(false);
        dataCommunicator.setDataProvider(dataProvider, null);
        // Use a limit value so that it's not multiple by page size
        Stream<Item> stream = dataCommunicator.fetchFromProvider(0, 123);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);

        Mockito.verify(dataProvider).fetch(queryCaptor.capture());

        List<Item> items = stream.collect(Collectors.toList());
        Assertions.assertEquals(123, items.size());

        Assertions.assertEquals(IntStream.range(0, 123).mapToObj(Item::new)
                .collect(Collectors.toList()), items);

        List<Query> allQueries = queryCaptor.getAllValues();
        Assertions.assertEquals(1, allQueries.size());

        Query query = allQueries.get(0);
        Assertions.assertEquals(0, query.getOffset());
        Assertions.assertEquals(123, query.getLimit());
        Assertions.assertEquals(0, query.getPage());
        Assertions.assertEquals(123, query.getPageSize());
    }

    @Test
    public void fetchFromProvider_maxLimitValue_pagesCalculatedProperly() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                42);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.setPageSize(2_000_000_000);
        // We check the page number calculation does not lead to integer
        // overflow, and not throw thus
        dataCommunicator.fetchFromProvider(0, Integer.MAX_VALUE);

        Mockito.verify(dataProvider, Mockito.times(1))
                .fetch(Mockito.any(Query.class));
    }

    @Test
    public void fetchFromProvider_backendRunsOutOfItems_secondPageRequestSkipped() {
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(
                42);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.fetchFromProvider(0, 100);

        // 42 < pageSize (50), so the second page shouldn't be requested
        Mockito.verify(dataProvider, Mockito.times(1))
                .fetch(Mockito.any(Query.class));
    }

    @Test
    public void fetchFromProvider_itemCountLessThanTwoPages_correctItemsReturned() {
        List<Item> items = new ArrayList<>();
        for (int i = 1; i <= 113; i++) {
            items.add(new Item(i));
        }

        DataProvider<Item, Void> dataProvider = DataProvider
                .fromCallbacks(query -> {
                    int offset = query.getPage() * query.getPageSize();
                    int end = offset + query.getPageSize();
                    if (end > items.size()) {
                        end = items.size();
                    }
                    return items.subList(offset, end).stream();
                }, query -> items.size());
        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.setPageSize(50);

        dataCommunicator.setDataProvider(dataProvider, null);
        // request second page with correct db size.
        Stream<Item> itemStream = dataCommunicator.fetchFromProvider(100, 13);
        List<Item> itemList = itemStream.toList();

        Assertions.assertEquals(13, itemList.size());
        Assertions.assertEquals(new Item(101), itemList.get(0));

    }

    @Test
    public void fetchFromProvider_itemCountLessThanTwoPages_getPageNotUsed_correctItemsReturned() {
        List<Item> items = new ArrayList<>();
        for (int i = 1; i <= 27; i++) {
            items.add(new Item(i));
        }

        DataProvider<Item, Void> dataProvider = DataProvider
                .fromCallbacks(query -> {
                    int end = query.getOffset() + query.getPageSize();
                    if (end > items.size()) {
                        end = items.size();
                    }
                    return items.subList(query.getOffset(), end).stream();
                }, query -> items.size());
        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.setPageSize(20);

        dataCommunicator.setDataProvider(dataProvider, null);
        // request second page with correct db size.
        Stream<Item> itemStream = dataCommunicator.fetchFromProvider(20, 7);
        List<Item> itemList = itemStream.toList();

        Assertions.assertEquals(7, itemList.size());
        Assertions.assertEquals(new Item(21), itemList.get(0));

    }

    @Test
    public void fetchFromProvider_streamIsClosed() {
        AtomicBoolean streamIsClosed = new AtomicBoolean();
        dataCommunicator.setDataProvider(createDataProvider(streamIsClosed),
                null);
        dataCommunicator.setViewportRange(0, 50);

        fakeClientCommunication();

        Assertions.assertTrue(streamIsClosed.get());
    }

    @Test
    public void fetchEnabled_getItemCount_stillReturnsItemsCount() {
        dataCommunicator.setFetchEnabled(false);
        Assertions.assertEquals(0, dataCommunicator.getItemCount());

        // data provider stores 100 items
        dataCommunicator.setDataProvider(createDataProvider(), null);
        Assertions.assertEquals(100, dataCommunicator.getItemCount());
    }

    @Test
    public void fetchEnabled_getItem_stillReturnsItem() {
        dataCommunicator.setFetchEnabled(false);

        // data provider stores 100 items
        dataCommunicator.setDataProvider(createDataProvider(), null);
        Assertions.assertNotNull(dataCommunicator.getItem(42));
    }

    @Test
    public void fetchEnabled_requestRange_fetchIgnored() {
        DataCommunicator<Item> dataCommunicator = new DataCommunicator<>(
                dataGenerator, arrayUpdater, data -> {
                }, element.getNode(), false);

        DataProvider<Item, ?> dataProvider = Mockito
                .spy(DataProvider.ofItems(new Item(0)));

        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.setViewportRange(0, 0);

        fakeClientCommunication();

        Mockito.verify(dataProvider, Mockito.times(0))
                .fetch(Mockito.any(Query.class));
        Mockito.verify(dataProvider, Mockito.times(0))
                .size(Mockito.any(Query.class));

        // Switch back to normal mode
        dataCommunicator.setFetchEnabled(true);
        dataCommunicator.setViewportRange(0, 10);

        fakeClientCommunication();

        Mockito.verify(dataProvider).fetch(Mockito.any(Query.class));
        Mockito.verify(dataProvider).size(Mockito.any(Query.class));
    }

    @Test
    public void setPageSize_setIncorrectPageSize_throws() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dataCommunicator.setPageSize(0));
        Assertions.assertTrue(ex.getMessage()
                .contains("Page size cannot be less than 1, got 0"));
    }

    @Test
    public void filter_setFilterThroughFilterConsumer_shouldRetainFilterBetweenRequests() {
        SerializableConsumer<SerializablePredicate<Item>> filterConsumer = dataCommunicator
                .setDataProvider(DataProvider.ofItems(new Item(1), new Item(2),
                        new Item(3)), item -> item.id > 1);

        Assertions.assertNotNull(dataCommunicator.getFilter(),
                "Expected initial filter to be set");

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertNotNull(dataCommunicator.getFilter(),
                "Filter should be retained after data request");

        Assertions.assertEquals(2, dataCommunicator.getItemCount(),
                "Unexpected items count");

        // Check that the filter change works properly
        filterConsumer.accept(item -> item.id > 2);

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertNotNull(dataCommunicator.getFilter(),
                "Filter should be retained after data request");

        Assertions.assertEquals(1, dataCommunicator.getItemCount(),
                "Unexpected items count");
    }

    @Test
    public void filter_setNotifyOnFilterChange_firesItemChangeEvent() {
        TestComponent testComponent = new TestComponent(element);

        AtomicBoolean eventTriggered = new AtomicBoolean(false);

        testComponent.addItemChangeListener(event -> {
            eventTriggered.set(true);
            Assertions.assertEquals(2, event.getItemCount(),
                    "Unexpected item count");
        });

        dataCommunicator.setDataProvider(
                DataProvider.ofItems(new Item(1), new Item(2), new Item(3)),
                item -> item.id > 1);

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assertions.assertTrue(eventTriggered.get(),
                "Expected event to be triggered");
    }

    @Test
    public void filter_skipNotifyOnFilterChange_doesNotFireItemChangeEvent() {
        TestComponent testComponent = new TestComponent(element);

        testComponent.addItemChangeListener(
                event -> Assertions.fail("Event triggering not expected"));

        dataCommunicator.setDataProvider(
                DataProvider.ofItems(new Item(1), new Item(2), new Item(3)),
                item -> item.id > 1, false);

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();
    }

    @Test
    public void setDataProvider_setNewDataProvider_filteringAndSortingRemoved() {
        dataCommunicator.setDataProvider(
                DataProvider.ofItems(new Item(0), new Item(1), new Item(2)),
                null);

        ListDataView<Item, ?> listDataView = new AbstractListDataView<Item>(
                dataCommunicator::getDataProvider, new TestComponent(element),
                (filter, sorting) -> {
                }) {
        };

        Assertions.assertEquals(3, listDataView.getItems().count(),
                "Unexpected items count before filter");
        Assertions.assertEquals(new Item(0),
                listDataView.getItems().findFirst().orElse(null),
                "Unexpected items order before sorting");

        listDataView.setFilter(item -> item.id < 2);
        listDataView.setSortOrder(item -> item.id, SortDirection.DESCENDING);

        Assertions.assertEquals(2, listDataView.getItems().count(),
                "Unexpected items count after filter");
        Assertions.assertEquals(new Item(1),
                listDataView.getItems().findFirst().orElse(null),
                "Unexpected items order after sorting");

        dataCommunicator.setDataProvider(
                DataProvider.ofItems(new Item(0), new Item(1), new Item(2)),
                null);

        Assertions.assertEquals(3, listDataView.getItems().count(),
                "Unexpected items count after data provider reset");
        Assertions.assertEquals(new Item(0),
                listDataView.getItems().findFirst().orElse(null),
                "Unexpected items order after data provider reset");
    }

    @Test // for https://github.com/vaadin/flow/issues/9988
    public void lazyLoadingFiltering_filterAppliedAfterScrolling_requestedRangeResolvedProperly() {
        final AtomicReference<Item> filter = new AtomicReference<>(null);

        final DataProvider<Item, ?> dataProvider = DataProvider.fromCallbacks(
                query -> IntStream.range(0, 1000)
                        .mapToObj(counter -> new Item(counter,
                                String.valueOf(counter)))
                        .filter(item -> filter.get() == null
                                || filter.get().equals(item))
                        .limit(query.getLimit()).skip(query.getOffset()),
                query -> {
                    Assertions
                            .fail("Count query is not expected in this test.");
                    return 0;
                });

        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.setDefinedSize(false);

        // Scroll forward to populate the DC cache (active items)
        dataCommunicator.setViewportRange(0, 100);
        fakeClientCommunication();

        dataCommunicator.setViewportRange(100, 150);
        fakeClientCommunication();

        // Apply the filter and force the requested range to be shifted back
        // towards to zero. This case should be handled properly without any
        // exception.
        filter.set(new Item(42, "42"));
        dataCommunicator.getDataProvider().refreshAll();
        fakeClientCommunication();

        // Check the filtered item
        Assertions.assertEquals(1, dataCommunicator.getItemCount(),
                "Expected 1 item after the filtering");
        Assertions.assertEquals(filter.get(), dataCommunicator.getItem(0),
                "Expected the item with value 42");
    }

    @Test
    public void handleAttach_componentAttached_oldDataProviderListenerRemoved() {
        // given
        AtomicInteger listenerInvocationCounter = new AtomicInteger(0);

        TestComponent componentWithDataProvider = new TestComponent(
                new Element("div"));
        dataCommunicator = new DataCommunicator<Item>(dataGenerator,
                arrayUpdater, data -> {
                }, componentWithDataProvider.getElement().getNode()) {
            @Override
            public void reset() {
                listenerInvocationCounter.incrementAndGet();
                super.reset();
            }
        };
        dataCommunicator.setViewportRange(0, 100);
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider();
        dataCommunicator.setDataProvider(dataProvider, null);
        // Add the component to a parent to trigger handle the attach event
        ui.add(componentWithDataProvider);
        fakeClientCommunication();

        Assertions.assertEquals(2, listenerInvocationCounter.get(),
                "Expected two DataCommunicator::reset() invocations: upon "
                        + "setting the data provider and component attaching");

        // when
        // the data is being refreshed -> data provider's listeners are being
        // invoked
        dataProvider.refreshAll();
        fakeClientCommunication();

        // then
        Assertions.assertEquals(3, listenerInvocationCounter.get(),
                "Expected only one reset() invocation, because the old "
                        + "listener was removed and then only one listener is stored");
    }

    @Test
    public void setViewportRange_defaultPageSize_tooMuchItemsRequested_maxItemsAllowedRequested() {
        DataProvider<Item, Object> dataProvider = Mockito
                .spy(createDataProvider(1000));
        dataCommunicator.setDataProvider(dataProvider, null);
        // Paging is disabled for easier check of requested amount of items
        dataCommunicator.setPagingEnabled(false);
        // More than allowed (500) items requested
        dataCommunicator.setViewportRange(0, 501);
        fakeClientCommunication();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);
        Mockito.verify(dataProvider, Mockito.times(1))
                .fetch(queryCaptor.capture());

        Assertions.assertEquals(500, queryCaptor.getValue().getLimit(),
                "Expected the requested items count to be limited"
                        + " to allowed threshold");
    }

    @Test
    public void setSmallPageSize_fetchMaximumItemsLowerLimit() {
        DataProvider<Item, Object> dataProvider = Mockito
                .spy(createDataProvider(1000));
        dataCommunicator.setDataProvider(dataProvider, null);
        dataCommunicator.setPageSize(2);
        dataCommunicator.setViewportRange(0, 1000);
        fakeClientCommunication();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);
        // to fetch 500 items 250 requests to dataProvider are required
        Mockito.verify(dataProvider, Mockito.times(250))
                .fetch(queryCaptor.capture());
    }

    @Test
    public void setViewportRange_customPageSize_customPageSizeConsidered_itemsRequested() {
        int newPageSize = 300;
        dataCommunicator.setPageSize(newPageSize);

        DataProvider<Item, Object> dataProvider = Mockito
                .spy(createDataProvider(1000));
        dataCommunicator.setDataProvider(dataProvider, null);
        // Paging is disabled for easier check of requested amount of items
        dataCommunicator.setPagingEnabled(false);
        dataCommunicator.setViewportRange(0, newPageSize * 2);
        fakeClientCommunication();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);

        Mockito.verify(dataProvider, Mockito.times(1))
                .fetch(queryCaptor.capture());

        Assertions.assertEquals(600, queryCaptor.getValue().getLimit(),
                "Expected two pages with page size = 300 to be "
                        + "requested and not limited");
    }

    // Simulates a flush request enqueued during a page reload with
    // @PreserveOnRefresh
    // see https://github.com/vaadin/flow/issues/14067
    @Test
    public void reattach_differentUI_requestFlushExecuted() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setViewportRange(0, 50);

        MockUI newUI = new MockUI();
        // simulates preserve on refresh
        // DataCommunicator has a flushRequest pending
        // that should be rescheduled on the new state tree
        newUI.getInternals().moveElementsFrom(ui);
        ui = newUI;
        fakeClientCommunication();

        Assertions.assertEquals(Range.withLength(0, 50), lastSet,
                "Expected initial full reset.");
    }

    @Tag("test-component")
    private static class TestComponent extends Component {

        public TestComponent() {
        }

        public TestComponent(Element element) {
            super(element);
        }

        void addItemChangeListener(
                ComponentEventListener<ItemCountChangeEvent<?>> listener) {
            ComponentUtil.addListener(this, ItemCountChangeEvent.class,
                    (ComponentEventListener) listener);
        }
    }

    private int getPageSizeIncrease() {
        return dataCommunicator.getPageSize() * 4;
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }

    private AbstractDataProvider<Item, Object> createDataProviderThatChangesSize(
            final int size, final int delta) {
        return new AbstractDataProvider<Item, Object>() {
            private boolean modifiedCount;

            @Override
            public boolean isInMemory() {
                return true;
            }

            @Override
            public int size(Query<Item, Object> query) {
                if (modifiedCount) {
                    return size - delta;
                }
                return size;
            }

            @Override
            public Stream<Item> fetch(Query<Item, Object> query) {
                int count = query.getLimit() + query.getOffset();
                if (!modifiedCount) {
                    count -= delta;
                    modifiedCount = true;
                }
                return asParallelIfRequired(
                        IntStream.range(query.getOffset(), count))
                        .mapToObj(Item::new);
            }
        };
    }

    private AbstractDataProvider<Item, Object> createDataProvider() {
        return createDataProvider(new AtomicBoolean());
    }

    private AbstractDataProvider<Item, Object> createDataProvider(
            AtomicBoolean streamIsClosed) {
        return new AbstractDataProvider<Item, Object>() {
            @Override
            public boolean isInMemory() {
                return true;
            }

            @Override
            public int size(Query<Item, Object> query) {
                return 100;
            }

            @Override
            public Stream<Item> fetch(Query<Item, Object> query) {
                return asParallelIfRequired(IntStream.range(query.getOffset(),
                        query.getLimit() + query.getOffset()))
                        .mapToObj(Item::new)
                        .onClose(() -> streamIsClosed.set(true));
            }
        };
    }

    private AbstractDataProvider<Item, Object> createDataProvider(int size) {
        return new AbstractDataProvider<Item, Object>() {
            @Override
            public boolean isInMemory() {
                return true;
            }

            @Override
            public int size(Query<Item, Object> query) {
                return size;
            }

            @Override
            public Stream<Item> fetch(Query<Item, Object> query) {
                int end = Math.min(query.getRequestedRangeEnd(), size);
                return asParallelIfRequired(
                        IntStream.range(query.getOffset(), end))
                        .mapToObj(Item::new);
            }
        };
    }

    private IntStream asParallelIfRequired(IntStream stream) {
        if (isDataProviderWithParallelStream()) {
            return stream.parallel();
        }
        return stream;
    }

    public static class MockUI extends UI {

        public MockUI() {
            this(findOrcreateSession());
        }

        public MockUI(VaadinSession session) {
            getInternals().setSession(session);
            setCurrent(this);
        }

        @Override
        protected void init(VaadinRequest request) {
            // Do nothing
        }

        private static VaadinSession findOrcreateSession() {
            VaadinSession session = VaadinSession.getCurrent();
            if (session == null) {
                MockService service = Mockito.mock(MockService.class);
                Mockito.when(service.getRouteRegistry())
                        .thenReturn(Mockito.mock(RouteRegistry.class));
                session = new AlwaysLockedVaadinSession(service);
                VaadinSession.setCurrent(session);
            }
            return session;
        }
    }

    public static class MockService extends VaadinServletService {

        @Override
        public RouteRegistry getRouteRegistry() {
            return super.getRouteRegistry();
        }
    }

    public static class AlwaysLockedVaadinSession extends MockVaadinSession {

        public AlwaysLockedVaadinSession(VaadinService service) {
            super(service);
            lock();
        }

    }

    public static class MockVaadinSession extends VaadinSession {
        /*
         * Used to make sure there's at least one reference to the mock session
         * while it's locked. This is used to prevent the session from being
         * eaten by GC in tests where @BeforeEach creates a session and sets it
         * as the current instance without keeping any direct reference to it.
         * This pattern has a chance of leaking memory if the session is not
         * unlocked in the right way, but it should be acceptable for testing
         * use.
         */
        private static final ThreadLocal<MockVaadinSession> referenceKeeper = new ThreadLocal<>();

        public MockVaadinSession(VaadinService service) {
            super(service);
        }

        @Override
        public void close() {
            super.close();
            closeCount++;
        }

        public int getCloseCount() {
            return closeCount;
        }

        @Override
        public Lock getLockInstance() {
            return lock;
        }

        @Override
        public void lock() {
            super.lock();
            referenceKeeper.set(this);
        }

        @Override
        public void unlock() {
            super.unlock();
            referenceKeeper.remove();
        }

        private int closeCount;

        private ReentrantLock lock = new ReentrantLock();
    }

}
