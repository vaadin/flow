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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
        Assert.assertTrue(dataView.isDefinedSize());
        Assert.assertEquals(BackEndDataProvider.class,
                dataView.getSupportedDataProviderType());
        Assert.assertEquals(3, dataView.getSize());
        // no items are activated
        Assert.assertFalse(dataView.contains("foo"));

        dataView.withUndefinedSize();
        Assert.assertFalse(dataView.isDefinedSize());

        dataView.withDefinedSize(query -> 5);
        Assert.assertTrue(dataView.isDefinedSize());

        dataView.withUndefinedSize(query -> 123);
        Assert.assertFalse(dataView.isDefinedSize());

        dataView.withDefinedSize();
        Assert.assertTrue(dataView.isDefinedSize());

        dataView.withUndefinedSize(500);
        Assert.assertFalse(dataView.isDefinedSize());

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
        dataView.withUndefinedSize();
    }

    @Test
    public void contains_itemsNotFetched_canCheckForItemWithBeforeClientResponse() {
        dataCommunicator.setRequestedRange(0, 50);

        Assert.assertFalse("Item should not be loaded yet",
                dataView.contains("foo"));

        AtomicBoolean capturedContains = new AtomicBoolean(false);
        ui.beforeClientResponse(component, executionContext -> {
            capturedContains.set(dataView.contains(ITEM1));
        });

        fakeClientCommunication();

        Assert.assertTrue("Item should be loaded", dataView.contains(ITEM1));
        Assert.assertTrue(
                "Item should be available during beforeClientResponse",
                capturedContains.get());
    }

    @Test
    public void size_withDefinedSize() {
        dataCommunicator.setRequestedRange(0, 50);
        Assert.assertEquals("Invalid size reported", 3, dataView.getSize());

        dataView.withDefinedSize(query -> 5);

        Assert.assertEquals("Invalid size reported", 5, dataView.getSize());
    }

    @Test
    public void size_withUndefinedSize() {
        dataCommunicator.setRequestedRange(0, 50);
        dataView.withUndefinedSize();

        Assert.assertEquals("Invalid size reported", 0, dataView.getSize());

        fakeClientCommunication();

        Assert.assertEquals("Invalid size reported", 3, dataView.getSize());

        dataView.withUndefinedSize(query -> 500);

        // since the size was "locked", there is no estimate
        Assert.assertEquals("Invalid size reported", 3, dataView.getSize());

        fakeClientCommunication();

        Assert.assertEquals("Invalid size reported", 3, dataView.getSize());

        // setting new data provider triggers new size to be applied after
        // communication
        dataCommunicator.setDataProvider(DataProvider.fromCallbacks(query -> {
            query.getOffset();
            return Stream.generate(String::new).limit(query.getLimit());
        }, query -> -1), null);
        dataView.withUndefinedSize(query -> 400);

        Assert.assertEquals("Invalid size reported", 0, dataView.getSize());

        fakeClientCommunication();

        Assert.assertEquals("Invalid size reported", 400, dataView.getSize());

        dataView.withUndefinedSize(300);

        Assert.assertEquals("Invalid size reported", 0, dataView.getSize());

        fakeClientCommunication();

        Assert.assertEquals("Invalid size reported", 300, dataView.getSize());
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

        dataCommunicator.setSizeEstimateCallback(query -> 66);

        fakeClientCommunication();

        Assert.assertEquals(66, dataView.getSize());

        limit.set(70);
        Stream<String> items = dataView.getItems();
        Assert.assertEquals("Invalid amount of items returned", limit.get(),
                items.count());
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }
}
