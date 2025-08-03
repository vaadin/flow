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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalArrayUpdater.HierarchicalUpdate;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HierarchicalCommunicatorTest {

    private static final String ROOT = "ROOT";
    private static final String FOLDER = "FOLDER";
    private static final String LEAF = "LEAF";
    private TreeDataProvider<String> dataProvider;
    private HierarchicalDataCommunicator<String> communicator;
    private TreeData<String> treeData;
    private UI ui;
    private UIInternals uiInternals;
    private StateTree stateTree;
    private final int pageSize = 50;
    private StateNode stateNode;

    private List<String> enqueueFunctions = new ArrayList<>();

    private Map<String, Serializable[]> enqueueFunctionsWithParams = new HashMap<>();

    private class UpdateQueue implements HierarchicalUpdate {
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
            enqueueFunctions.add(name);
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

    private class UpdateQueueWithArguments extends UpdateQueue {
        @Override
        public void enqueue(String name, Serializable... arguments) {
            enqueueFunctionsWithParams.put(name, arguments);
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

    private final HierarchicalArrayUpdater arrayUpdaterWithArguments = new HierarchicalArrayUpdater() {
        @Override
        public HierarchicalUpdate startUpdate(int sizeChange) {
            return new UpdateQueueWithArguments();
        }

        @Override
        public void initialize() {
        }
    };

    @Before
    public void setUp() {
        ui = Mockito.mock(UI.class);
        uiInternals = Mockito.mock(UIInternals.class);
        stateTree = Mockito.mock(StateTree.class);
        Mockito.when(ui.getInternals()).thenReturn(uiInternals);
        Mockito.when(uiInternals.getStateTree()).thenReturn(stateTree);

        treeData = new TreeData<>();
        treeData.addItems(null, ROOT);
        treeData.addItems(ROOT, FOLDER);
        treeData.addItems(FOLDER, LEAF);
        dataProvider = new TreeDataProvider<>(treeData);
        stateNode = Mockito.mock(StateNode.class);
        Mockito.when(stateNode.hasFeature(Mockito.any())).thenReturn(true);
        ComponentMapping mapping = Mockito.mock(ComponentMapping.class);
        Mockito.when(stateNode.getFeatureIfInitialized(ComponentMapping.class))
                .thenReturn(java.util.Optional.ofNullable(mapping));
        Mockito.when(mapping.getComponent())
                .thenReturn(java.util.Optional.of(new TestComponent()));
        communicator = new HierarchicalDataCommunicator<>(
                Mockito.mock(CompositeDataGenerator.class), arrayUpdater,
                json -> {
                }, stateNode, () -> null);
        communicator.setDataProvider(dataProvider, null);
    }

    @Test
    public void folderRemoveRefreshAll() {
        testItemRemove(FOLDER, true);
    }

    @Test
    public void leafRemoveRefreshAll() {
        testItemRemove(LEAF, true);
    }

    @Test
    public void folderRemove() {
        testItemRemove(FOLDER, false);
    }

    @Test
    public void leafRemove() {
        testItemRemove(LEAF, false);
    }

    private void testItemRemove(String item, boolean refreshAll) {
        communicator.expand(ROOT);
        communicator.expand(FOLDER);
        // Put the item into client queue
        communicator.refresh(item);
        treeData.removeItem(item);
        if (refreshAll) {
            dataProvider.refreshAll();
        } else {
            dataProvider.refreshItem(item);
        }

        int number = refreshAll ? 7 : 6;

        ArgumentCaptor<SerializableConsumer> attachCaptor = ArgumentCaptor
                .forClass(SerializableConsumer.class);
        Mockito.verify(stateNode, Mockito.times(number))
                .runWhenAttached(attachCaptor.capture());

        attachCaptor.getAllValues().forEach(consumer -> consumer.accept(ui));

        Mockito.verify(stateTree, Mockito.times(number))
                .beforeClientResponse(Mockito.any(), Mockito.any());
    }

    @Test
    public void replaceAll() {
        // Some modifications
        communicator.expand(ROOT);
        communicator.expand(FOLDER);
        communicator.refresh(LEAF);
        // Replace dataprovider
        communicator.setDataProvider(new TreeDataProvider<>(new TreeData<>()),
                null);
        dataProvider.refreshAll();
        assertFalse("Stalled object in KeyMapper",
                communicator.getKeyMapper().has(ROOT));
    }

    /**
     * Test for ensuring that when moving a root node to be a child node of
     * another root node, the key is held in KeyMapper after two flush events.
     *
     * Related: <a href="https://github.com/vaadin/flow/issues/14351">Original
     * issue</a> Related:
     * <a href="https://github.com/vaadin/flow/pull/17774">Fix</a> Related:
     * <a href="https://github.com/vaadin/flow-components/pull/5545">Integration
     * Test</a>
     */
    @Test
    public void moveNodeFromRootToChildAndFlushTwice_keyShouldBeInKeyMapper() {
        final String secondRoot = "SECONDROOT";

        treeData.addItem(null, secondRoot);
        communicator.setViewportRange(0, 2);

        invokeFlush();

        dataProvider.getTreeData().setParent(secondRoot, ROOT);
        communicator.expand(ROOT);
        dataProvider.refreshAll();

        communicator.confirmUpdate(1);
        invokeFlush();

        assertTrue("SECONDROOT key is missing from KeyMapper",
                communicator.getKeyMapper().has(secondRoot));
    }

    private void invokeFlush() {
        try {
            Method flush = DataCommunicator.class.getDeclaredMethod("flush");
            flush.setAccessible(true);
            flush.invoke(communicator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void reset_noDataControllers_hierarchicalUpdateIsCalled() {
        enqueueFunctions.clear();
        // The communicator is just initialized with a data provider and has no
        // any data controllers
        communicator.reset();

        Assert.assertEquals(1, enqueueFunctions.size());
        Assert.assertEquals("$connector.ensureHierarchy",
                enqueueFunctions.get(0));
    }

    @Test
    public void reset_expandSomeItems_hierarchicalUpdateContainsExpandItems() {
        enqueueFunctions.clear();

        communicator.expand(ROOT);

        communicator.reset();

        // One expandItems for calling expand(...)
        // One expandItems and one ensureHierarchy for calling reset()
        Assert.assertEquals(3, enqueueFunctions.size());
        Assert.assertEquals("$connector.expandItems", enqueueFunctions.get(0));
        Assert.assertEquals("$connector.ensureHierarchy",
                enqueueFunctions.get(1));
        Assert.assertEquals("$connector.expandItems", enqueueFunctions.get(2));
    }

    @Test
    public void reset_expandSomeItems_updateContainsProperJsonObjectsToExpand() {
        enqueueFunctionsWithParams = new HashMap<>();

        TreeData<String> hierarchyTreeData = new TreeData<>();
        hierarchyTreeData.addItem(null, "root");
        hierarchyTreeData.addItem("root", "first-1");
        hierarchyTreeData.addItem("root", "first-2");
        hierarchyTreeData.addItem("first-1", "second-1-1");
        hierarchyTreeData.addItem("first-2", "second-2-1");

        TreeDataProvider<String> treeDataProvider = new TreeDataProvider<>(
                hierarchyTreeData);

        HierarchicalDataCommunicator<String> dataCommunicator = new HierarchicalDataCommunicator<String>(
                Mockito.mock(CompositeDataGenerator.class),
                arrayUpdaterWithArguments, json -> {
                }, stateNode, () -> null);

        dataCommunicator.setDataProvider(treeDataProvider, null);

        dataCommunicator.expand("root");
        dataCommunicator.expand("first-1");

        dataCommunicator.reset();

        Assert.assertTrue(enqueueFunctionsWithParams
                .containsKey("$connector.expandItems"));
        JsonArray arguments = (JsonArray) enqueueFunctionsWithParams
                .get("$connector.expandItems")[0];
        Assert.assertNotNull(arguments);
        Assert.assertEquals(2, arguments.length());

        JsonObject first1 = arguments.getObject(0);
        JsonObject root = arguments.getObject(1);

        Assert.assertNotNull(first1);
        Assert.assertNotNull(root);

        Assert.assertTrue(first1.hasKey("key"));
        Assert.assertTrue(root.hasKey("key"));

        Assert.assertEquals(dataCommunicator.getKeyMapper().key("first-1"),
                first1.getString("key"));
        Assert.assertEquals(dataCommunicator.getKeyMapper().key("root"),
                root.getString("key"));
    }

    @Tag("test")
    public static class TestComponent extends Component {
    }
}
