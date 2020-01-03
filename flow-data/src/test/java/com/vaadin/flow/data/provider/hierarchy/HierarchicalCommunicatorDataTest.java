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
package com.vaadin.flow.data.provider.hierarchy;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataCommunicatorTest;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalArrayUpdater.HierarchicalUpdate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import elemental.json.JsonValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    private static class UpdateQueue implements HierarchicalUpdate {
        @Override
        public void clear(int start, int length) {
        }

        @Override
        public void set(int start, List<JsonValue> items) {
        }

        @Override
        public void commit(int updateId) {
        }

        @Override
        public void enqueue(String name, Serializable... arguments) {
        }

        @Override
        public void set(int start, List<JsonValue> items, String parentKey) {
        }

        @Override
        public void clear(int start, int length, String parentKey) {
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
        Element element = new Element("div");
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
                session = new DataCommunicatorTest.AlwaysLockedVaadinSession(
                        null);
                VaadinSession.setCurrent(session);
            }
            return session;
        }
    }

    public static class AlwaysLockedVaadinSession
            extends DataCommunicatorTest.MockVaadinSession {

        public AlwaysLockedVaadinSession(VaadinService service) {
            super(service);
            lock();
        }

    }

}
