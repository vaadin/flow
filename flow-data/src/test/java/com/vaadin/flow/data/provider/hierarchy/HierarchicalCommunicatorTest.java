/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.data.provider.ArrayUpdater;
import com.vaadin.flow.data.provider.ArrayUpdater.Update;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;

import elemental.json.JsonValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

    private class UpdateQueue implements Update {
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
        public void set(int start, List<JsonValue> items,
                String parentKey) {
        }

        @Override
        public void clear(int start, int length,
                String parentKey) {
        }

        @Override
        public void commit(int updateId, String parentKey, int levelSize) {
        }

        @Override
        public void commit() {
        }
    }

    private final ArrayUpdater arrayUpdater = new ArrayUpdater() {
        @Override
        public Update startUpdate(int sizeChange) {
            return new UpdateQueue();
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
        communicator = new HierarchicalDataCommunicator<>(
                Mockito.mock(CompositeDataGenerator.class),
                arrayUpdater, json -> {
                },
                stateNode, () -> null);
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
        
        ArgumentCaptor<SerializableConsumer> attachCaptor = ArgumentCaptor
                .forClass(SerializableConsumer.class);
        Mockito.verify(stateNode, Mockito.times(4))
                .runWhenAttached(attachCaptor.capture());

        attachCaptor.getAllValues().forEach(consumer -> consumer.accept(ui));

        Mockito.verify(stateTree, Mockito.times(4))
                .beforeClientResponse(Mockito.any(),
                Mockito.any());
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
        assertEquals(-1, communicator.getParentIndex(FOLDER).longValue());
    }

}
