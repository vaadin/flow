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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableConsumer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import elemental.json.JsonValue;

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
            public void set(int start, List<JsonValue> items) {

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
        dataCommunicator.setPageSize(50);
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

        dataView.setItemCountCallback(query -> 5);
        Assert.assertTrue(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountEstimate(500);
        Assert.assertFalse(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountFromDataProvider();
        Assert.assertTrue(dataView.getDataCommunicator().isDefinedSize());

        dataView.setItemCountEstimateIncrease(200);
        Assert.assertFalse(dataView.getDataCommunicator().isDefinedSize());
    }

    // TODO https://github.com/vaadin/flow/issues/8583
    @Test(expected = IllegalStateException.class)
    public void dataViewCreated_beforeSettingDataProvider_throws() {
        // data communicator has by default an empty list data provider ->
        // utilizing lazy data view fails
        new AbstractLazyDataView<String>(
                new DataCommunicator<>((item, jsonObject) -> {
                }, null, null, component.getElement().getNode()), component) {
        };
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
        dataCommunicator.setRequestedRange(0, 50);

        Assert.assertEquals("Invalid item count reported", 0, itemCount.get());

        fakeClientCommunication();

        Assert.assertEquals("Invalid item count reported", 3, itemCount.get());

        dataView.setItemCountCallback(query -> 2);

        fakeClientCommunication();

        Assert.assertEquals("Invalid item count reported", 2, itemCount.get());
    }

    @Test
    public void itemCount_undefinedItemCount() {
        final AtomicInteger itemCount = new AtomicInteger(0);
        dataView.addItemCountChangeListener(
                event -> itemCount.set(event.getItemCount()));
        dataCommunicator.setRequestedRange(0, 50);
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

        Assert.assertEquals("Invalid item count reported", 3,
                itemCount.get());

        fakeClientCommunication();

        Assert.assertEquals("Invalid item count reported", 2,
                itemCount.get());

        dataView.setItemCountEstimate(300);

        Assert.assertEquals("Invalid item count reported", 2, itemCount.get());

        fakeClientCommunication();

        Assert.assertEquals("Invalid item count reported", 300,
                itemCount.get());
    }

    @Test
    public void getItems_withDefinedSize() {
        dataCommunicator.setRequestedRange(0, 50);
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
        dataCommunicator.setRequestedRange(0, 50);
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
    public void getItem_withDefinedSizeAndCorrectIndex() {
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> 3), null);

        fakeClientCommunication();

        // Request the item within the active range
        Assert.assertEquals("Invalid item on index 0", "foo",
                dataView.getItem(0));

        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(
                query -> Stream.generate(String::new).skip(query.getOffset())
                        .limit(query.getLimit()),
                query -> 300), null);

        fakeClientCommunication();

        // Request an item outside the active range
        Assert.assertNotNull(dataView.getItem(260));
    }

    @Test
    public void getItem_withDefinedSizeAndNegativeIndex() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage(
                "Given index -1 is outside of the accepted range '0 - 2'");
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> 3), null);

        fakeClientCommunication();

        dataView.getItem(-1);
    }

    @Test
    public void getItem_withDefinedSizeAndEmptyDataset() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage("Requested index 0 on empty data.");
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.empty();
        }, query -> 0), null);

        fakeClientCommunication();

        dataView.getItem(0);
    }

    @Test
    public void getItem_withDefinedSizeAndIndexOutsideOfRange() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage(
                "Given index 3 is outside of the accepted range '0 - 2'");
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> 3), null);

        fakeClientCommunication();

        Assert.assertEquals("Invalid item on index 0", "foo",
                dataView.getItem(3));
    }

    @Test
    public void getItem_withDefinedSizeAndFiltering() {
        final String initialFilter = "bar";
        final String newFilter = "foo";

        dataCommunicator.setRequestedRange(0, 50);
        SerializableConsumer<String> newFilterProvider = dataCommunicator
                .setDataProvider(DataProvider.fromFilteringCallbacks(query -> {
                    query.getOffset();
                    query.getLimit();
                    return Stream.of("foo", "bar", "baz").filter(
                            item -> item.equals(query.getFilter().get()));
                }, query -> 1), initialFilter);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", initialFilter,
                dataView.getItem(0));

        newFilterProvider.accept(newFilter);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", newFilter,
                dataView.getItem(0));
    }

    @Test
    public void getItem_withDefinedSizeAndSorting() {
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            Stream<String> stream = Stream.of("foo", "bar", "baz");
            if (query.getInMemorySorting() != null) {
                stream = stream.sorted(query.getInMemorySorting());
            }
            return stream;
        }, query -> 3), null);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", "foo",
                dataView.getItem(0));

        dataCommunicator.setInMemorySorting(String::compareTo);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", "bar",
                dataView.getItem(0));
    }

    @Test
    public void getItem_withUndefinedSizeAndCorrectIndex() {
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> -1), null);

        dataCommunicator.setItemCountEstimate(5);
        fakeClientCommunication();

        // Request the item within the active range
        Assert.assertEquals("Wrong item on index 0", "foo",
                dataView.getItem(0));
        Assert.assertEquals("Wrong item on index 2", "baz",
                dataView.getItem(2));

        // Request the item outside the active range
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            if (query.getOffset() > 70) {
                return Stream.empty();
            } else {
                return Stream.generate(String::new).skip(query.getOffset())
                        .limit(Math.min(query.getOffset() + query.getLimit(),
                                70));
            }
        }, query -> -1), null);

        final int itemCountEstimate = 60;
        dataCommunicator.setItemCountEstimate(itemCountEstimate);

        fakeClientCommunication();
        Assert.assertNotNull(dataView.getItem(49));
        Assert.assertNotNull(dataView.getItem(59));
        Assert.assertNotNull(dataView.getItem(69));
        Assert.assertNull(dataView.getItem(79));
    }

    @Test
    public void getItem_withUndefinedSizeAndFiltering() {
        final String initialFilter = "bar";
        final String newFilter = "foo";

        dataCommunicator.setRequestedRange(0, 50);
        SerializableConsumer<String> newFilterProvider = dataCommunicator
                .setDataProvider(DataProvider.fromFilteringCallbacks(query -> {
                    query.getOffset();
                    query.getLimit();
                    return Stream.of("foo", "bar", "baz").filter(
                            item -> item.equals(query.getFilter().get()));
                }, query -> -1), initialFilter);

        dataCommunicator.setItemCountEstimate(5);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", initialFilter,
                dataView.getItem(0));

        newFilterProvider.accept(newFilter);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", newFilter,
                dataView.getItem(0));
    }

    @Test
    public void getItem_withUndefinedSizeAndEmptyDataset() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage("Requested index 0 on empty data.");
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.empty();
        }, query -> -1), null);

        dataCommunicator.setItemCountEstimate(2);
        fakeClientCommunication();

        dataView.getItem(0);
    }

    @Test
    public void getItem_withUndefinedSizeAndIndexOutsideOfRange() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage(
                "Given index 3 is outside of the accepted range '0 - 2'");
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> -1), null);

        dataCommunicator.setItemCountEstimate(3);
        fakeClientCommunication();

        dataView.getItem(3);
    }

    @Test
    public void getItem_withUndefinedSizeAndSorting() {
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            Stream<String> stream = Stream.of("foo", "bar", "baz");
            if (query.getInMemorySorting() != null) {
                stream = stream.sorted(query.getInMemorySorting());
            }
            return stream;
        }, query -> -1), null);

        dataCommunicator.setItemCountEstimate(5);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", "foo",
                dataView.getItem(0));

        dataCommunicator.setInMemorySorting(String::compareTo);

        fakeClientCommunication();
        Assert.assertEquals("Invalid item on index 0", "bar",
                dataView.getItem(0));
    }

    @Test
    public void getItem_withUndefinedSizeAndNegativeIndex() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage(
                "Given index -1 is outside of the accepted range '0 - 2'");
        dataCommunicator.setRequestedRange(0, 50);
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            query.getLimit();
            return Stream.of("foo", "bar", "baz");
        }, query -> -1), null);

        dataCommunicator.setItemCountEstimate(1);
        fakeClientCommunication();

        dataView.getItem(-1);
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }
}
