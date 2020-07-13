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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import elemental.json.JsonValue;

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
            public void set(int start, List<JsonValue> items) {
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
        pageSize = 50;
        dataCommunicator.setPageSize(pageSize);
    }

    @Test
    public void communicator_with_0_items_should_not_refresh_all() {
        dataCommunicator.setRequestedRange(0, 0);
        fakeClientCommunication();

        Assert.assertEquals(Range.withLength(0, 0), lastSet);
        Assert.assertNull(
                "Only requestAll should clear items. This may make us loop.",
                lastClear);

        dataCommunicator.setRequestedRange(0, 0);
        fakeClientCommunication();

        Assert.assertEquals(Range.withLength(0, 0), lastSet);
        Assert.assertNull(
                "Only requestAll should clear items. Which would make us loop.",
                lastClear);
    }

    @Test
    public void communicator_with_items_should_send_updates_but_not_refresh_all() {
        dataCommunicator.setDataProvider(createDataProvider(), null);

        dataCommunicator.setRequestedRange(0, 50);
        fakeClientCommunication();

        Assert.assertEquals(
                "Expected request range for 50 items on first request.",
                Range.withLength(0, 50), lastSet);

        dataCommunicator.setRequestedRange(0, 70);
        fakeClientCommunication();

        Assert.assertEquals("Expected request range for 20 new items.",
                Range.withLength(50, 20), lastSet);
    }

    @Test
    public void reattach_different_roundtrip_refresh_all() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
    public void setDataProvider_keyMapperIsReset() {
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.setRequestedRange(0, 50);
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
                .fromCallbacks(query -> {
                    return items.stream();
                }, query -> {
                    return items.size();
                });
        dataCommunicator.setDataProvider(dataProvider, null);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(CoreMatchers.containsString(
                "The data provider hasn't ever called getLimit or " +
                        "getPageSize"));
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
                "The data provider hasn't ever called getOffset or getPage"));
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

        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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

        dataCommunicator.setRequestedRange(50, 50);

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
        dataCommunicator.setRequestedRange(150, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
        Assert.assertFalse(dataCommunicator.isDefinedSize());

        fakeClientCommunication();

        Assert.assertEquals("initial size estimate not used",
                initialCountEstimate, dataCommunicator.getItemCount());
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        Mockito.verify(dataProvider, Mockito.times(1)).fetch(Mockito.any());

        dataCommunicator.setRequestedRange(50, 50);

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
        dataCommunicator.setRequestedRange(0, 50);
        fakeClientCommunication();

        final int initialCountEstimate = 111;
        dataCommunicator.setItemCountEstimate(initialCountEstimate);
        Assert.assertFalse(dataCommunicator.isDefinedSize());

        dataCommunicator.setRequestedRange(50, 100);
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
        dataCommunicator.setRequestedRange(0, requestedRangeEnd);

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
        dataCommunicator.setRequestedRange(400, rangeLength);

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
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(exactSize);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        final int itemCountEstimate = 1000;
        dataCommunicator.setItemCountEstimate(itemCountEstimate);
        dataCommunicator.setRequestedRange(0, 50);
        fakeClientCommunication();

        Assert.assertEquals(itemCountEstimate, dataCommunicator.getItemCount());

        // if the user scrolls far from the exact size of the backend,
        // the exact size is resolved on the server side without causing a new
        // roundtrip where the client will request items because it received less
        // items than expected
        dataCommunicator.setRequestedRange(900, 100);
        fakeClientCommunication();

        Assert.assertEquals(exactSize, dataCommunicator.getItemCount());
        Mockito.verify(dataProvider, Mockito.times(0)).size(Mockito.any());
        // 1. initial call 0-50, 2. then: 900-950, 950-1000, 800-850, 850-900,
        // ... 100-150, 150-200
        Mockito.verify(dataProvider, Mockito.times(19)).fetch(Mockito.any());
    }

    @Test
    public void setInitialItemCountEstimateAndIncrease_backendEmpty_noEndlessFlushLoop() {
        final int exactSize = 0;
        AbstractDataProvider<Item, Object> dataProvider = createDataProvider(exactSize);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        final int itemCountEstimate = 1000;
        dataCommunicator.setItemCountEstimate(itemCountEstimate);
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
        fakeClientCommunication();
        Assert.assertEquals("Wrong active item", new Item(0),
                dataCommunicator.getItem(0));
        Assert.assertEquals("Wrong active item", new Item(49),
                dataCommunicator.getItem(49));

        dataCommunicator.setRequestedRange(50, 50);
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
        dataCommunicator.setRequestedRange(0, 50);

        Assert.assertFalse("Item should not be active",
                dataCommunicator.isItemActive(new Item(0)));

        fakeClientCommunication();

        Assert.assertTrue("Item should be active",
                dataCommunicator.isItemActive(new Item(0)));
        Assert.assertTrue("Item should be active",
                dataCommunicator.isItemActive(new Item(49)));
        Assert.assertFalse("Item should not be active",
                dataCommunicator.isItemActive(new Item(50)));

        dataCommunicator.setRequestedRange(50, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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

        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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

        dataCommunicator.setRequestedRange(100, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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

        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
        pageSize = 50;
        dataCommunicator.setPageSize(pageSize);
        AtomicReference<ItemCountChangeEvent<?>> cachedEvent = new AtomicReference<>();
        ComponentUtil.addListener(component, ItemCountChangeEvent.class,
                ((ComponentEventListener) event -> {
                    Assert.assertNull(cachedEvent.get());
                    cachedEvent.set((ItemCountChangeEvent<?>) event);
                }));
        int exactCount = 500;
        dataCommunicator.setDataProvider(createDataProvider(exactCount), null);
        dataCommunicator.setRequestedRange(0, 50);
        // exact size
        fakeClientCommunication();

        ItemCountChangeEvent<?> event = cachedEvent.getAndSet(null);

        Assert.assertEquals("Invalid count provided", exactCount,
                event.getItemCount());
        Assert.assertFalse(event.isItemCountEstimated());
        // no new event fired
        dataCommunicator.setRequestedRange(450, 50);
        fakeClientCommunication();
        Assert.assertNull(cachedEvent.get());

        // creating a new data provider with same exact size -> no new event fired
        dataCommunicator.setDataProvider(createDataProvider(500), null);
        fakeClientCommunication();
        Assert.assertNull(cachedEvent.get());

        // new data provider with different size
        dataCommunicator.setDataProvider(createDataProvider(exactCount = 1000), null);
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
        pageSize = 50;
        dataCommunicator.setPageSize(pageSize);
        AtomicReference<ItemCountChangeEvent<?>> cachedEvent = new AtomicReference<>();
        ComponentUtil.addListener(component, ItemCountChangeEvent.class,
                ((ComponentEventListener) event -> {
                    Assert.assertNull(cachedEvent.get());
                    cachedEvent.set((ItemCountChangeEvent<?>) event);
                }));
        int exactCount = 500;
        dataCommunicator.setDataProvider(createDataProvider(exactCount), null);
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDefinedSize(false);
        // initial estimate count of 200
        fakeClientCommunication();

        ItemCountChangeEvent<?> event = cachedEvent.getAndSet(null);
        Assert.assertEquals("Invalid count provided", 200,
                event.getItemCount());
        Assert.assertTrue(event.isItemCountEstimated());

        dataCommunicator.setRequestedRange(150, 50);
        fakeClientCommunication();

        event = cachedEvent.getAndSet(null);
        Assert.assertEquals("Invalid count provided", 400,
                event.getItemCount());
        Assert.assertTrue(event.isItemCountEstimated());

        dataCommunicator.setRequestedRange(350, 50);
        fakeClientCommunication();

        event = cachedEvent.getAndSet(null);
        Assert.assertEquals("Invalid count provided", 600,
                event.getItemCount());
        Assert.assertTrue(event.isItemCountEstimated());

        dataCommunicator.setRequestedRange(550, 50);
        fakeClientCommunication();

        // reaching exact size
        event = cachedEvent.getAndSet(null);
        Assert.assertEquals("Invalid count provided", 500,
                event.getItemCount());
        Assert.assertFalse(event.isItemCountEstimated());
    }

    @Test
    public void fetchFromProvider_pageSizeLessThanLimit_multiplePagedQueries() {
        AbstractDataProvider<Item, Object> dataProvider =
                createDataProvider(100);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setPageSize(10);
        dataCommunicator.setDataProvider(dataProvider, null);
        Stream<Item> stream = dataCommunicator.fetchFromProvider(0, 30);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);

        Mockito.verify(dataProvider, Mockito.times(3)).fetch(
                queryCaptor.capture());

        List<Item> items = stream.collect(Collectors.toList());
        Assert.assertEquals(30, items.size());

        Assert.assertEquals(
                IntStream.range(0, 30).mapToObj(Item::new)
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
    }

    @Test
    public void fetchFromProvider_limitEqualsPageSize_singleQuery() {
        AbstractDataProvider<Item, Object> dataProvider =
                createDataProvider(100);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        Stream<Item> stream = dataCommunicator.fetchFromProvider(0, 50);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);

        Mockito.verify(dataProvider).fetch(queryCaptor.capture());

        List<Item> items = stream.collect(Collectors.toList());
        Assert.assertEquals(50, items.size());

        Assert.assertEquals(
                IntStream.range(0, 50).mapToObj(Item::new)
                        .collect(Collectors.toList()), items);

        Query query = queryCaptor.getValue();
        Assert.assertEquals(0, query.getOffset());
        Assert.assertEquals(50, query.getLimit());
        Assert.assertEquals(0, query.getPage());
        Assert.assertEquals(50, query.getPageSize());
    }

    @Test
    public void fetchFromProvider_limitLessThanPageSize_singleQuery() {
        AbstractDataProvider<Item, Object> dataProvider =
                createDataProvider(100);
        dataProvider = Mockito.spy(dataProvider);

        dataCommunicator.setDataProvider(dataProvider, null);
        Stream<Item> stream = dataCommunicator.fetchFromProvider(10, 40);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor
                .forClass(Query.class);

        Mockito.verify(dataProvider).fetch(queryCaptor.capture());

        List<Item> items = stream.collect(Collectors.toList());
        Assert.assertEquals(50, items.size());

        Assert.assertEquals(
                IntStream.range(10, 60).mapToObj(Item::new)
                        .collect(Collectors.toList()), items);

        Query query = queryCaptor.getValue();
        Assert.assertEquals(10, query.getOffset());
        Assert.assertEquals(50, query.getLimit());
        Assert.assertEquals(0, query.getPage());
        Assert.assertEquals(50, query.getPageSize());
    }

    @Tag("test-component")
    private static class TestComponent extends Component {
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
                return IntStream.range(query.getOffset(), count)
                        .mapToObj(Item::new);
            }
        };
    }

    private AbstractDataProvider<Item, Object> createDataProvider() {
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
                return IntStream
                        .range(query.getOffset(),
                                query.getLimit() + query.getOffset())
                        .mapToObj(Item::new);
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
                return IntStream.range(query.getOffset(), end)
                        .mapToObj(Item::new);
            }
        };
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
                session = new AlwaysLockedVaadinSession(null);
                VaadinSession.setCurrent(session);
            }
            return session;
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
