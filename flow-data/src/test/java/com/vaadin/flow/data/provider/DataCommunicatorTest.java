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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

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

@RunWith(Parameterized.class)
public class DataCommunicatorTest {
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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

    private final boolean dataProviderWithParallelStream;

    public DataCommunicatorTest(boolean dataProviderWithParallelStream) {
        this.dataProviderWithParallelStream = dataProviderWithParallelStream;
    }

    @Parameterized.Parameters
    public static Collection<Boolean> testParameters() {
        // Runs tests with both sequential and parallel data provider streams
        return Arrays.asList(false, true);
    }

    @Before
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

        Assert.assertEquals(Range.withLength(0, 0), lastSet);
        Assert.assertNull(
                "Only requestAll should clear items. This may make us loop.",
                lastClear);

        dataCommunicator.setViewportRange(0, 0);
        fakeClientCommunication();

        Assert.assertEquals(Range.withLength(0, 0), lastSet);
        Assert.assertNull(
                "Only requestAll should clear items. Which would make us loop.",
                lastClear);
    }

    @Test
    public void communicator_with_items_should_send_updates_but_not_refresh_all() {
        dataCommunicator.setDataProvider(createDataProvider(), null);

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assert.assertEquals(
                "Expected request range for 50 items on first request.",
                Range.withLength(0, 50), lastSet);

        dataCommunicator.setViewportRange(0, 70);
        fakeClientCommunication();

        Assert.assertEquals("Expected request range for 20 new items.",
                Range.withLength(50, 20), lastSet);
    }

    @Test
    public void reattach_different_roundtrip_refresh_all() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assert.assertEquals("Expected initial full reset.",
                Range.withLength(0, 50), lastSet);
        lastSet = null;

        element.removeFromParent();
        fakeClientCommunication();

        Assert.assertNull("Expected no during reattach.", lastSet);

        ui.getElement().appendChild(element);
        fakeClientCommunication();

        Assert.assertEquals("Expected initial full reset after reattach",
                Range.withLength(0, 50), lastSet);
    }

    @Test
    public void reattach_same_roundtrip_refresh_nothing() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assert.assertEquals("Expected initial full reset.",
                Range.withLength(0, 50), lastSet);
        lastSet = null;

        element.removeFromParent();

        Assert.assertNull("Expected no communication during reattach", lastSet);

        ui.getElement().appendChild(element);
        fakeClientCommunication();

        Assert.assertNull("Expected no communication after reattach", lastSet);
    }

    @Test
    public void setFlushRequest_remove_setFlushRequest_reattach_noEndlessFlushLoop() {
        AtomicInteger listenerInvocationCounter = new AtomicInteger(0);
        dataCommunicator = new DataCommunicator<>(dataGenerator, arrayUpdater,
                data -> {
                }, element.getNode()) {
            @Override
            public void reset() {
                Assert.assertTrue("Should not fall into endless reset loop",
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

        Assert.assertEquals(0, dataCommunicator.getKeyMapper().get("1").id);

        dataCommunicator.setDataProvider(createDataProvider(), null);
        Assert.assertNull(
                "The KeyMapper should be reset when a new DataProvider is set",
                dataCommunicator.getKeyMapper().get("1"));
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

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(CoreMatchers.containsString(
                "The data provider hasn't ever called getLimit() or "
                        + "getPageSize()"));
        dataCommunicator.fetchFromProvider(0, 1);
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

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(CoreMatchers.containsString(
                "The data provider hasn't ever called getOffset() or getPage()"));
        dataCommunicator.fetchFromProvider(1, 1);
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
        Assert.assertEquals(2,
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

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(CoreMatchers.containsString(
                "exceeds the limit specified by the query (4)."));

        stream.forEach(item -> {
        });
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

        Assert.assertSame(originalItem,
                dataCommunicator.getKeyMapper().get(key));

        Item updatedItem = new Item(originalItem.id, "Updated");
        items.set(0, updatedItem);
        dataProvider.refreshAll();

        fakeClientCommunication();

        Assert.assertSame(updatedItem,
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

        Assert.assertEquals(40, lastSet.getEnd());
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
        Assert.assertTrue(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        AtomicBoolean sizeCallbackCall = new AtomicBoolean(false);
        dataCommunicator.setCountCallback(query -> {
            sizeCallbackCall.set(true);
            return 100;
        });
        Assert.assertTrue(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        Assert.assertTrue("SizeCallback not called",
                sizeCallbackCall.getAndSet(false));
        Assert.assertEquals("Size not used", 100,
                dataCommunicator.getItemCount());

        Mockito.verify(dataProvider, Mockito.times(1)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());

        dataCommunicator.setViewportRange(50, 50);

        fakeClientCommunication();

        Mockito.verify(dataProvider, Mockito.times(1)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(2)).fetch(Mockito.any());

        Assert.assertFalse("SizeCallback called when should not have",
                sizeCallbackCall.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setSizeCallback_null_throws() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setCountCallback(null);
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
        Assert.assertFalse(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        Assert.assertEquals("initial estimate+increase not used",
                itemCountEstimate + itemCountEstimateIncrease,
                dataCommunicator.getItemCount());
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());

        AtomicBoolean sizeCallbackCall = new AtomicBoolean(false);
        final int exactSize = 1234;
        dataCommunicator.setCountCallback(query -> {
            sizeCallbackCall.set(true);
            return exactSize;
        });
        Assert.assertTrue(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        Assert.assertTrue("SizeCallback not called",
                sizeCallbackCall.getAndSet(false));
        Assert.assertEquals("Size not used", exactSize,
                dataCommunicator.getItemCount());
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
        Assert.assertFalse(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        Assert.assertEquals("initial size estimate not used",
                initialCountEstimate, dataCommunicator.getItemCount());
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());

        dataCommunicator.setViewportRange(50, 50);

        fakeClientCommunication();

        Assert.assertEquals("initial size estimate was not discarded",
                initialCountEstimate + getPageSizeIncrease(),
                dataCommunicator.getItemCount());
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
        Assert.assertFalse(dataCommunicator.isDefinedSize());

        dataCommunicator.setViewportRange(50, 100);
        fakeClientCommunication();

        Assert.assertEquals(
                "too small initial size estimate should not be applied",
                initialCountEstimate + getPageSizeIncrease(),
                dataCommunicator.getItemCount());
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
        Assert.assertEquals(
                "Size should be automatically adjusted for too small estimate",
                initialCountEstimate + getPageSizeIncrease(),
                dataCommunicator.getItemCount());
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
        Assert.assertEquals(
                "Size should be automatically adjusted for too small estimate",
                initialCountEstimate + (3 * itemCountEstimateIncrease),
                dataCommunicator.getItemCount());
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

        Assert.assertEquals(itemCountEstimate, dataCommunicator.getItemCount());

        // if the user scrolls far from the exact size of the backend,
        // the exact size is resolved on the server side without causing a new
        // roundtrip where the client will request items because it received
        // less
        // items than expected
        dataCommunicator.setViewportRange(900, 100);
        fakeClientCommunication();

        Assert.assertEquals(exactSize, dataCommunicator.getItemCount());
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

        Assert.assertEquals(exactSize, dataCommunicator.getItemCount());
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setInitialCountEstimate_lessThanOne_throws() {
        dataCommunicator.setItemCountEstimate(0);
    }

    @Test
    public void getActiveItemOnIndex_activeRangeChanges_itemsReturned() {
        dataCommunicator.setDataProvider(createDataProvider(300), null);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();
        Assert.assertEquals("Wrong active item", new Item(0),
                dataCommunicator.getItem(0));
        Assert.assertEquals("Wrong active item", new Item(49),
                dataCommunicator.getItem(49));

        dataCommunicator.setViewportRange(50, 50);
        fakeClientCommunication();

        Assert.assertEquals("Wrong active item", new Item(50),
                dataCommunicator.getItem(50));
        Assert.assertEquals("Wrong active item", new Item(69),
                dataCommunicator.getItem(69));
        Assert.assertEquals("Wrong active item", new Item(99),
                dataCommunicator.getItem(99));
    }

    @Test
    public void isItemActive_newItems() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setViewportRange(0, 50);

        Assert.assertFalse("Item should not be active",
                dataCommunicator.isItemActive(new Item(0)));

        fakeClientCommunication();

        Assert.assertTrue("Item should be active",
                dataCommunicator.isItemActive(new Item(0)));
        Assert.assertTrue("Item should be active",
                dataCommunicator.isItemActive(new Item(49)));
        Assert.assertFalse("Item should not be active",
                dataCommunicator.isItemActive(new Item(50)));

        dataCommunicator.setViewportRange(50, 50);
        fakeClientCommunication();

        Assert.assertTrue("Item should be active",
                dataCommunicator.isItemActive(new Item(50)));
        Assert.assertTrue("Item should be active",
                dataCommunicator.isItemActive(new Item(99)));
        Assert.assertFalse("Item should not be active",
                dataCommunicator.isItemActive(new Item(100)));
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
        Assert.assertEquals("Invalid item on index 1", new Item(1),
                dataCommunicator.getItem(1));

        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(
                query -> IntStream.range(0, 300).mapToObj(Item::new)
                        .skip(query.getOffset()).limit(query.getLimit()),
                query -> 300), null);

        fakeClientCommunication();

        // Request an item outside the active range
        Assert.assertEquals("Invalid item on index 260", new Item(260),
                dataCommunicator.getItem(260));
    }

    @Test
    public void getItem_withDefinedSizeAndNegativeIndex() {
        expectedException.expect(IndexOutOfBoundsException.class);
        expectedException.expectMessage("Index must be non-negative");
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of(new Item(0));
        }, query -> 1), null);

        fakeClientCommunication();
        dataCommunicator.getItem(-1);
    }

    @Test
    public void getItem_withDefinedSizeAndEmptyDataset() {
        expectedException.expect(IndexOutOfBoundsException.class);
        expectedException.expectMessage("Requested index 0 on empty data.");
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.empty();
        }, query -> 0), null);

        fakeClientCommunication();
        dataCommunicator.getItem(0);
    }

    @Test
    public void getItem_withDefinedSizeAndIndexOutsideOfRange() {
        expectedException.expect(IndexOutOfBoundsException.class);
        expectedException.expectMessage(
                "Given index 3 is outside of the accepted range '0 - 2'");
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return IntStream.of(0, 1, 2).mapToObj(Item::new);
        }, query -> 3), null);

        fakeClientCommunication();
        dataCommunicator.getItem(3);
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
        Assert.assertEquals("Invalid item on index 0", initialFilter,
                dataCommunicator.getItem(0));

        newFilterProvider.accept(newFilter);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", newFilter,
                dataCommunicator.getItem(0));
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
        Assert.assertEquals("Invalid item on index 0", new Item(1),
                dataCommunicator.getItem(0));
        Assert.assertEquals("Invalid item on index 1", new Item(2),
                dataCommunicator.getItem(1));
        Assert.assertEquals("Invalid item on index 2", new Item(0),
                dataCommunicator.getItem(2));

        dataCommunicator
                .setInMemorySorting((i1, i2) -> Integer.compare(i1.id, i2.id));

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", new Item(0),
                dataCommunicator.getItem(0));
        Assert.assertEquals("Invalid item on index 1", new Item(1),
                dataCommunicator.getItem(1));
        Assert.assertEquals("Invalid item on index 2", new Item(2),
                dataCommunicator.getItem(2));
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
        Assert.assertEquals("Wrong item on index 0", new Item(0),
                dataCommunicator.getItem(0));
        Assert.assertEquals("Wrong item on index 1", new Item(1),
                dataCommunicator.getItem(1));

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
        Assert.assertEquals("Wrong item on index 375", new Item(375),
                dataCommunicator.getItem(375));

        // Request the item outside the active range, and outside the
        // estimation (and present in the backend)
        Assert.assertEquals("Wrong item on index 450", new Item(450),
                dataCommunicator.getItem(450));
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
        Assert.assertEquals("Invalid item on index 1", new Item(1),
                dataCommunicator.getItem(1));

        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.empty();
        }, query -> -1), null);

        dataCommunicator.setItemCountEstimate(2);
        fakeClientCommunication();
        // This checks the situation when the fetch actions has happened
        // but the data set is empty
        Assert.assertNull("Item on index 0 supposed to be null",
                dataCommunicator.getItem(0));
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
        Assert.assertEquals("Invalid item on index 3", new Item(3),
                dataCommunicator.getItem(3));

        // Index 5 is outside of estimation and not in the backend
        Assert.assertNull("Item on index 5 supposed to be null",
                dataCommunicator.getItem(5));
    }

    @Test
    public void getItem_withUndefinedSizeAndNegativeIndex() {
        expectedException.expect(IndexOutOfBoundsException.class);
        expectedException.expectMessage("Index must be non-negative");
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of(new Item(0));
        }, query -> -1), null);

        dataCommunicator.setItemCountEstimate(1);
        fakeClientCommunication();
        dataCommunicator.getItem(-1);
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
        Assert.assertEquals("Invalid item on index 0", initialFilter,
                dataCommunicator.getItem(0));

        newFilterProvider.accept(newFilter);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", newFilter,
                dataCommunicator.getItem(0));
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
        Assert.assertEquals("Invalid item on index 0", new Item(1),
                dataCommunicator.getItem(0));
        Assert.assertEquals("Invalid item on index 1", new Item(2),
                dataCommunicator.getItem(1));
        Assert.assertEquals("Invalid item on index 2", new Item(0),
                dataCommunicator.getItem(2));

        dataCommunicator
                .setInMemorySorting((i1, i2) -> Integer.compare(i1.id, i2.id));

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", new Item(0),
                dataCommunicator.getItem(0));
        Assert.assertEquals("Invalid item on index 1", new Item(1),
                dataCommunicator.getItem(1));
        Assert.assertEquals("Invalid item on index 2", new Item(2),
                dataCommunicator.getItem(2));
    }

    @Test
    public void getItem_streamIsClosed() {
        AtomicBoolean streamIsClosed = new AtomicBoolean();
        dataCommunicator.setDataProvider(createDataProvider(streamIsClosed),
                null);

        fakeClientCommunication();
        dataCommunicator.getItem(0);

        Assert.assertTrue(streamIsClosed.get());
    }

    @Test
    public void itemCountEstimateAndStep_defaults() {
        Assert.assertEquals(dataCommunicator.getItemCountEstimate(),
                pageSize * 4);
        Assert.assertEquals(dataCommunicator.getItemCountEstimateIncrease(),
                pageSize * 4);

        int customPageSize = 100;
        dataCommunicator.setPageSize(customPageSize);

        Assert.assertEquals(dataCommunicator.getItemCountEstimate(),
                customPageSize * 4);
        Assert.assertEquals(dataCommunicator.getItemCountEstimateIncrease(),
                customPageSize * 4);

        int customItemCountEstimate = 123;
        dataCommunicator.setItemCountEstimate(customItemCountEstimate);
        int customItemCountEstimateStep = 456;
        dataCommunicator
                .setItemCountEstimateIncrease(customItemCountEstimateStep);

        Assert.assertEquals(dataCommunicator.getItemCountEstimate(),
                customItemCountEstimate);
        Assert.assertEquals(dataCommunicator.getItemCountEstimateIncrease(),
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
                    Assert.assertNull(cachedEvent.get());
                    cachedEvent.set((ItemCountChangeEvent<?>) event);
                }));
        int exactCount = 500;
        dataCommunicator.setDataProvider(createDataProvider(exactCount), null);
        dataCommunicator.setViewportRange(0, 50);
        // exact size
        fakeClientCommunication();

        ItemCountChangeEvent<?> event = cachedEvent.getAndSet(null);

        Assert.assertEquals("Invalid count provided", exactCount,
                event.getItemCount());
        Assert.assertFalse(event.isItemCountEstimated());
        // no new event fired
        dataCommunicator.setViewportRange(450, 50);
        fakeClientCommunication();
        Assert.assertNull(cachedEvent.get());

        // creating a new data provider with same exact size -> no new event
        // fired
        dataCommunicator.setDataProvider(createDataProvider(500), null);
        fakeClientCommunication();
        Assert.assertNull(cachedEvent.get());

        // new data provider with different size
        dataCommunicator.setDataProvider(createDataProvider(exactCount = 1000),
                null);
        fakeClientCommunication();
        event = cachedEvent.getAndSet(null);

        Assert.assertEquals("Invalid count provided", exactCount,
                event.getItemCount());
        Assert.assertFalse(event.isItemCountEstimated());
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
                    Assert.assertNull(cachedEvent.get());
                    cachedEvent.set((ItemCountChangeEvent<?>) event);
                }));
        int exactCount = 500;
        dataCommunicator.setDataProvider(createDataProvider(exactCount), null);
        dataCommunicator.setViewportRange(0, 50);
        dataCommunicator.setDefinedSize(false);
        // initial estimate count of 200
        fakeClientCommunication();

        ItemCountChangeEvent<?> event = cachedEvent.getAndSet(null);
        Assert.assertEquals("Invalid count provided", 200,
                event.getItemCount());
        Assert.assertTrue(event.isItemCountEstimated());

        dataCommunicator.setViewportRange(150, 50);
        fakeClientCommunication();

        event = cachedEvent.getAndSet(null);
        Assert.assertEquals("Invalid count provided", 400,
                event.getItemCount());
        Assert.assertTrue(event.isItemCountEstimated());

        dataCommunicator.setViewportRange(350, 50);
        fakeClientCommunication();

        event = cachedEvent.getAndSet(null);
        Assert.assertEquals("Invalid count provided", 600,
                event.getItemCount());
        Assert.assertTrue(event.isItemCountEstimated());

        dataCommunicator.setViewportRange(550, 50);
        fakeClientCommunication();

        // reaching exact size
        event = cachedEvent.getAndSet(null);
        Assert.assertEquals("Invalid count provided", 500,
                event.getItemCount());
        Assert.assertFalse(event.isItemCountEstimated());
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
        Assert.assertEquals(300, dataCommunicator.getItemCount());
    }

    @Test
    public void pageSize_defaultPageSizeUsed_returnsItemNormally() {
        dataCommunicator.setDataProvider(DataProvider.ofItems(new Item(0)),
                null);
        Stream<Item> itemStream = dataCommunicator.fetchFromProvider(0, 100);
        Assert.assertNotNull(itemStream);
        Assert.assertEquals(1, itemStream.count());
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
        Assert.assertEquals(30, items.size());

        Assert.assertEquals(IntStream.range(0, 30).mapToObj(Item::new)
                .collect(Collectors.toList()), items);

        List<Query> allQueries = queryCaptor.getAllValues();
        Assert.assertEquals(3, allQueries.size());

        Query query = allQueries.get(0);
        Assert.assertEquals(0, query.getOffset());
        Assert.assertEquals(10, query.getLimit());
        Assert.assertEquals(0, query.getPage());
        Assert.assertEquals(10, query.getPageSize());

        query = allQueries.get(1);
        Assert.assertEquals(10, query.getOffset());
        Assert.assertEquals(10, query.getLimit());
        Assert.assertEquals(1, query.getPage());
        Assert.assertEquals(10, query.getPageSize());

        query = allQueries.get(2);
        Assert.assertEquals(20, query.getOffset());
        Assert.assertEquals(10, query.getLimit());
        Assert.assertEquals(2, query.getPage());
        Assert.assertEquals(10, query.getPageSize());

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
        Assert.assertEquals(50, items.size());

        Assert.assertEquals(IntStream.range(0, 50).mapToObj(Item::new)
                .collect(Collectors.toList()), items);

        Query query = queryCaptor.getValue();
        Assert.assertEquals(0, query.getOffset());
        Assert.assertEquals(50, query.getLimit());
        Assert.assertEquals(0, query.getPage());
        Assert.assertEquals(50, query.getPageSize());
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
        Assert.assertEquals(42, items.size());

        Assert.assertEquals(IntStream.range(10, 52).mapToObj(Item::new)
                .collect(Collectors.toList()), items);

        Query query = queryCaptor.getValue();
        Assert.assertEquals(10, query.getOffset());
        Assert.assertEquals(42, query.getLimit());
        Assert.assertEquals(0, query.getPage());
        Assert.assertEquals(42, query.getPageSize());
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
        Assert.assertEquals(123, items.size());

        Assert.assertEquals(IntStream.range(0, 123).mapToObj(Item::new)
                .collect(Collectors.toList()), items);

        List<Query> allQueries = queryCaptor.getAllValues();
        Assert.assertEquals(1, allQueries.size());

        Query query = allQueries.get(0);
        Assert.assertEquals(0, query.getOffset());
        Assert.assertEquals(123, query.getLimit());
        Assert.assertEquals(0, query.getPage());
        Assert.assertEquals(123, query.getPageSize());
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

        Assert.assertEquals(13, itemList.size());
        Assert.assertEquals(new Item(101), itemList.get(0));

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

        Assert.assertEquals(7, itemList.size());
        Assert.assertEquals(new Item(21), itemList.get(0));

    }

    @Test
    public void fetchFromProvider_streamIsClosed() {
        AtomicBoolean streamIsClosed = new AtomicBoolean();
        dataCommunicator.setDataProvider(createDataProvider(streamIsClosed),
                null);
        dataCommunicator.setViewportRange(0, 50);

        fakeClientCommunication();

        Assert.assertTrue(streamIsClosed.get());
    }

    @Test
    public void fetchEnabled_getItemCount_stillReturnsItemsCount() {
        dataCommunicator.setFetchEnabled(false);
        Assert.assertEquals(0, dataCommunicator.getItemCount());

        // data provider stores 100 items
        dataCommunicator.setDataProvider(createDataProvider(), null);
        Assert.assertEquals(100, dataCommunicator.getItemCount());
    }

    @Test
    public void fetchEnabled_getItem_stillReturnsItem() {
        dataCommunicator.setFetchEnabled(false);

        // data provider stores 100 items
        dataCommunicator.setDataProvider(createDataProvider(), null);
        Assert.assertNotNull(dataCommunicator.getItem(42));
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
        expectedException.expect(IllegalArgumentException.class);
        expectedException
                .expectMessage("Page size cannot be less than 1, got 0");
        dataCommunicator.setPageSize(0);
    }

    @Test
    public void filter_setFilterThroughFilterConsumer_shouldRetainFilterBetweenRequests() {
        SerializableConsumer<SerializablePredicate<Item>> filterConsumer = dataCommunicator
                .setDataProvider(DataProvider.ofItems(new Item(1), new Item(2),
                        new Item(3)), item -> item.id > 1);

        Assert.assertNotNull("Expected initial filter to be set",
                dataCommunicator.getFilter());

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assert.assertNotNull("Filter should be retained after data request",
                dataCommunicator.getFilter());

        Assert.assertEquals("Unexpected items count", 2,
                dataCommunicator.getItemCount());

        // Check that the filter change works properly
        filterConsumer.accept(item -> item.id > 2);

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assert.assertNotNull("Filter should be retained after data request",
                dataCommunicator.getFilter());

        Assert.assertEquals("Unexpected items count", 1,
                dataCommunicator.getItemCount());
    }

    @Test
    public void filter_setNotifyOnFilterChange_firesItemChangeEvent() {
        TestComponent testComponent = new TestComponent(element);

        AtomicBoolean eventTriggered = new AtomicBoolean(false);

        testComponent.addItemChangeListener(event -> {
            eventTriggered.set(true);
            Assert.assertEquals("Unexpected item count", 2,
                    event.getItemCount());
        });

        dataCommunicator.setDataProvider(
                DataProvider.ofItems(new Item(1), new Item(2), new Item(3)),
                item -> item.id > 1);

        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assert.assertTrue("Expected event to be triggered",
                eventTriggered.get());
    }

    @Test
    public void filter_skipNotifyOnFilterChange_doesNotFireItemChangeEvent() {
        TestComponent testComponent = new TestComponent(element);

        testComponent.addItemChangeListener(
                event -> Assert.fail("Event triggering not expected"));

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

        Assert.assertEquals("Unexpected items count before filter", 3,
                listDataView.getItems().count());
        Assert.assertEquals("Unexpected items order before sorting",
                new Item(0), listDataView.getItems().findFirst().orElse(null));

        listDataView.setFilter(item -> item.id < 2);
        listDataView.setSortOrder(item -> item.id, SortDirection.DESCENDING);

        Assert.assertEquals("Unexpected items count after filter", 2,
                listDataView.getItems().count());
        Assert.assertEquals("Unexpected items order after sorting", new Item(1),
                listDataView.getItems().findFirst().orElse(null));

        dataCommunicator.setDataProvider(
                DataProvider.ofItems(new Item(0), new Item(1), new Item(2)),
                null);

        Assert.assertEquals("Unexpected items count after data provider reset",
                3, listDataView.getItems().count());
        Assert.assertEquals("Unexpected items order after data provider reset",
                new Item(0), listDataView.getItems().findFirst().orElse(null));
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
                    Assert.fail("Count query is not expected in this test.");
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
        Assert.assertEquals("Expected 1 item after the filtering", 1,
                dataCommunicator.getItemCount());
        Assert.assertEquals("Expected the item with value 42", filter.get(),
                dataCommunicator.getItem(0));
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

        Assert.assertEquals(
                "Expected two DataCommunicator::reset() invocations: upon "
                        + "setting the data provider and component attaching",
                2, listenerInvocationCounter.get());

        // when
        // the data is being refreshed -> data provider's listeners are being
        // invoked
        dataProvider.refreshAll();
        fakeClientCommunication();

        // then
        Assert.assertEquals(
                "Expected only one reset() invocation, because the old "
                        + "listener was removed and then only one listener is stored",
                3, listenerInvocationCounter.get());
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

        Assert.assertEquals(
                "Expected the requested items count to be limited"
                        + " to allowed threshold",
                500, queryCaptor.getValue().getLimit());
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

        Assert.assertEquals(
                "Expected two pages with page size = 300 to be "
                        + "requested and not limited",
                600, queryCaptor.getValue().getLimit());
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

        Assert.assertEquals("Expected initial full reset.",
                Range.withLength(0, 50), lastSet);
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
        if (dataProviderWithParallelStream) {
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
         * eaten by GC in tests where @Before creates a session and sets it as
         * the current instance without keeping any direct reference to it. This
         * pattern has a chance of leaking memory if the session is not unlocked
         * in the right way, but it should be acceptable for testing use.
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
