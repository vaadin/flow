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
package com.vaadin.flow.data.provider.hierarchy;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataCommunicatorTest;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalArrayUpdater.HierarchicalUpdate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import elemental.json.JsonValue;

public class HierarchicalCommunicatorDataTest {
    /**
     * Test item that uses id for identity.
     */
    private static class Item {
        private final int id;
        private String value;

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

    private static final Item ROOT = new Item(0, "ROOT");
    private static final Item FOLDER = new Item(1, "FOLDER");
    private static final Item LEAF = new Item(2, "LEAF");
    private TreeDataProvider<Item> dataProvider;
    private HierarchicalDataCommunicator<Item> communicator;
    private TreeData<Item> treeData;
    private MockUI ui;
    private Element element;

    private boolean parentClearCalled = false;

    private int committedUpdateId;

    private class UpdateQueue implements HierarchicalUpdate {

        @Override
        public void clear(int start, int length) {
        }

        @Override
        public void set(int start, List<JsonValue> items) {
        }

        @Override
        public void commit(int updateId) {
            committedUpdateId = updateId;
        }

        @Override
        public void enqueue(String name, Serializable... arguments) {
        }

        @Override
        public void set(int start, List<JsonValue> items, String parentKey) {
        }

        @Override
        public void clear(int start, int length, String parentKey) {
            parentClearCalled = true;
        }

        @Override
        public void commit(int updateId, String parentKey, int levelSize) {
        }

        @Override
        public void commit() {
        }
    }

    private final HierarchicalArrayUpdater arrayUpdater = new HierarchicalArrayUpdater() {
        @Override
        public HierarchicalUpdate startUpdate(int sizeChange) {
            return new UpdateQueue();
        }

        @Override
        public void initialize() {
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ui = new MockUI();
        element = new Element("div");
        ui.getElement().appendChild(element);

        treeData = new TreeData<>();
        treeData.addItems(null, ROOT);
        treeData.addItems(ROOT, FOLDER);
        treeData.addItems(FOLDER, LEAF);
        dataProvider = new TreeDataProvider<>(treeData);
        communicator = new HierarchicalDataCommunicator<>(
                Mockito.mock(CompositeDataGenerator.class), arrayUpdater,
                json -> {
                }, element.getNode(),
                () -> (ValueProvider<Item, String>) item -> String
                        .valueOf(item.id));
        communicator.setDataProvider(dataProvider, null);
    }

    @Test
    public void sameKeyDifferentInstance_latestInstanceUsed() {
        communicator.expand(ROOT);
        communicator.expand(FOLDER);

        fakeClientCommunication();

        Item originalLeaf = treeData.getChildren(FOLDER).get(0);
        String key = communicator.getKeyMapper().key(originalLeaf);

        Assert.assertSame(originalLeaf, communicator.getKeyMapper().get(key));

        Item updatedLeaf = new Item(originalLeaf.id, "Updated");
        treeData.removeItem(LEAF);
        treeData.addItems(FOLDER, updatedLeaf);

        fakeClientCommunication();

        dataProvider.refreshItem(FOLDER, true);

        fakeClientCommunication();

        Assert.assertSame(updatedLeaf, communicator.getKeyMapper().get(key));
    }

    @Test
    public void uniqueKeyProviderIsSet_keysGeneratedByProvider() {
        communicator = new HierarchicalDataCommunicator<Item>(
                Mockito.mock(CompositeDataGenerator.class), arrayUpdater,
                json -> {
                }, element.getNode(), () -> item -> item.value);
        communicator.setDataProvider(dataProvider, null);
        // expand test items to force key calculation
        communicator.expand(ROOT);
        communicator.expand(FOLDER);
        // this is needed to force key calculation for leaf item
        communicator.setParentRequestedRange(0, 50, LEAF);
        fakeClientCommunication();

        Arrays.asList("ROOT", "FOLDER", "LEAF")
                .forEach(key -> Assert.assertNotNull("Expected key '" + key
                        + "' to be generated when unique key provider used",
                        communicator.getKeyMapper().get(key)));
    }

    @Test
    public void uniqueKeyProviderIsNotSet_keysGeneratedByKeyMapper() {
        communicator = new HierarchicalDataCommunicator<Item>(
                Mockito.mock(CompositeDataGenerator.class), arrayUpdater,
                json -> {
                }, element.getNode(), () -> null);
        communicator.setDataProvider(dataProvider, null);
        // expand test items to force key calculation
        communicator.expand(ROOT);
        communicator.expand(FOLDER);
        // this is needed to force key calculation for leaf item
        communicator.setParentRequestedRange(0, 50, LEAF);
        fakeClientCommunication();

        // key mapper should generate keys 1,2,3
        IntStream.range(1, 4).mapToObj(String::valueOf)
                .forEach(i -> Assert.assertNotNull("Expected key '" + i
                        + "' to be generated when unique key provider is not set",
                        communicator.getKeyMapper().get(i)));
    }

    @Test
    public void expandRoot_streamIsClosed() {
        AtomicBoolean streamIsClosed = new AtomicBoolean();

        dataProvider = new TreeDataProvider<>(treeData) {

            @Override
            public Stream<Item> fetchChildren(
                    HierarchicalQuery<Item, SerializablePredicate<Item>> query) {
                return super.fetchChildren(query)
                        .onClose(() -> streamIsClosed.set(true));
            }
        };

        communicator.setDataProvider(dataProvider, null);

        communicator.expand(ROOT);
        fakeClientCommunication();

        communicator.setParentRequestedRange(0, 50, ROOT);
        fakeClientCommunication();

        communicator.reset();

        Assert.assertTrue(streamIsClosed.get());
    }

    @Test
    public void expandRoot_filterOutAllChildren_clearCalled() {
        parentClearCalled = false;

        communicator.expand(ROOT);
        fakeClientCommunication();

        communicator.setParentRequestedRange(0, 50, ROOT);
        fakeClientCommunication();

        SerializablePredicate<Item> filter = item -> ROOT.equals(item);
        communicator.setFilter(filter);
        fakeClientCommunication();

        dataProvider.refreshItem(ROOT, true);
        fakeClientCommunication();

        communicator.reset();

        Assert.assertTrue(parentClearCalled);
    }

    @Test
    public void expandItem_requestNonOverlappingRange_expandedItemPersistsInKeyMapper() {
        committedUpdateId = -1;

        int indexToTest = 2;
        int requestedRangeLength = 5;

        treeData = new TreeData<>();
        for (int id = 0; id < requestedRangeLength * 4; id++) {
            treeData.addItems(null, new Item(id, "Item " + id));
        }
        Item itemToTest = treeData.getRootItems().get(indexToTest);
        treeData.addItems(itemToTest, new Item(treeData.getRootItems().size(),
                "Item " + indexToTest + "_" + 0));
        dataProvider = new TreeDataProvider<>(treeData);
        communicator.setDataProvider(dataProvider, null);

        communicator.setViewportRange(0, requestedRangeLength);
        fakeClientCommunication();
        Assert.assertTrue(communicator.getKeyMapper().has(itemToTest));
        String initialKey = communicator.getKeyMapper().key(itemToTest);

        communicator.expand(itemToTest);
        communicator.setViewportRange(requestedRangeLength * 2,
                requestedRangeLength);
        fakeClientCommunication();
        assertKeyItemPairIsPresentInKeyMapper(initialKey, itemToTest);

        communicator.confirmUpdate(committedUpdateId);
        fakeClientCommunication();
        assertKeyItemPairIsPresentInKeyMapper(initialKey, itemToTest);

        communicator.reset();
    }

    @Test
    public void expandItem_tooMuchItemsRequested_maxItemsAllowedRequested() {
        int startingChildId = 10000;
        int children = 1000;
        int maxAllowedItems = 500;
        treeData.clear();
        treeData.addItems(null, ROOT);
        treeData.addItems(ROOT, IntStream.range(0, children).mapToObj(
                i -> new Item(startingChildId + i, "ROOT CHILD " + i)));

        communicator.expand(ROOT);
        fakeClientCommunication();

        communicator.setParentRequestedRange(0, children, ROOT);
        fakeClientCommunication();

        IntStream.range(0, children).forEach(i -> {
            String treeItemId = Integer.toString(startingChildId + i);
            if (i < maxAllowedItems) {
                Assert.assertNotNull(
                        "Expecting item " + treeItemId
                                + " to be fetched, but was not",
                        communicator.getKeyMapper().get(treeItemId));
            } else {
                Assert.assertNull("Expecting item " + treeItemId
                        + " not to be fetched because of max allowed items rule, but it was fetched",
                        communicator.getKeyMapper().get(treeItemId));
            }
        });
    }

    private void assertKeyItemPairIsPresentInKeyMapper(String key, Item item) {
        Assert.assertTrue(communicator.getKeyMapper().has(item));
        Assert.assertEquals(key, communicator.getKeyMapper().key(item));
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }

    public static class MockUI extends UI {

        public MockUI() {
            this(findOrCreateSession());
        }

        public MockUI(VaadinSession session) {
            getInternals().setSession(session);
            setCurrent(this);
        }

        @Override
        protected void init(VaadinRequest request) {
            // Do nothing
        }

        private static VaadinSession findOrCreateSession() {
            VaadinSession session = VaadinSession.getCurrent();
            if (session == null) {
                DataCommunicatorTest.MockService service = Mockito
                        .mock(DataCommunicatorTest.MockService.class);
                Mockito.when(service.getRouteRegistry())
                        .thenReturn(Mockito.mock(RouteRegistry.class));
                session = new DataCommunicatorTest.AlwaysLockedVaadinSession(
                        service);
                VaadinSession.setCurrent(session);
            }
            return session;
        }
    }
}
