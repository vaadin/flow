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
package com.vaadin.flow.data.provider.hierarchy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.function.ValueProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchicalDataCommunicatorKeyGenerationTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    private ValueProvider<Item, String> uniqueKeyProvider = null;
    private HierarchicalDataCommunicator<Item> dataCommunicator;
    private DataKeyMapper<Item> keyMapper;

    @BeforeEach
    void init() {
        super.init();

        dataCommunicator = new HierarchicalDataCommunicator<>(
                new CompositeDataGenerator<Item>(), arrayUpdater,
                ui.getElement().getNode(), () -> uniqueKeyProvider);
        dataCommunicator.setDataProvider(treeDataProvider, null);

        keyMapper = dataCommunicator.getKeyMapper();
    }

    @Test
    void changeViewportRangeBackAndForth_generatedKeysMatchItems() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.expand(new Item("Item 0"));

        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 0")),
                keyMapper.key(new Item("Item 0-0")),
                keyMapper.key(new Item("Item 0-1")),
                keyMapper.key(new Item("Item 1")));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(98, 4);
        fakeClientCommunication();
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 96")),
                keyMapper.key(new Item("Item 97")),
                keyMapper.key(new Item("Item 98")),
                keyMapper.key(new Item("Item 99")));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 0")),
                keyMapper.key(new Item("Item 0-0")),
                keyMapper.key(new Item("Item 0-1")),
                keyMapper.key(new Item("Item 1")),
                keyMapper.key(new Item("Item 2")),
                keyMapper.key(new Item("Item 3")));
    }

    @Test
    void changeViewportRangeBackAndForth_reset_generatedKeysMatchItems() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.expand(new Item("Item 0"));

        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        dataCommunicator.setViewportRange(98, 4);
        fakeClientCommunication();
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.reset();
        fakeClientCommunication();
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 0")),
                keyMapper.key(new Item("Item 0-0")),
                keyMapper.key(new Item("Item 0-1")),
                keyMapper.key(new Item("Item 1")),
                keyMapper.key(new Item("Item 2")),
                keyMapper.key(new Item("Item 3")));
    }

    @Test
    void changeViewportRangeBackAndForth_noLongerVisibleKeysRemovedAfterConfirmation() {
        populateTreeData(treeData, 100, 2);
        dataCommunicator.expand(new Item("Item 0"));

        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        dataCommunicator.confirmUpdate(captureArrayUpdateId());
        assertTrue(keyMapper.has(new Item("Item 0")));
        assertTrue(keyMapper.has(new Item("Item 0-0")));
        assertTrue(keyMapper.has(new Item("Item 0-1")));
        assertTrue(keyMapper.has(new Item("Item 1")));
        assertFalse(keyMapper.has(new Item("Item 2")));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(2, 4);
        fakeClientCommunication(); // not confirmed yet
        assertTrue(keyMapper.has(new Item("Item 0")));
        assertTrue(keyMapper.has(new Item("Item 0-0")));

        dataCommunicator.confirmUpdate(captureArrayUpdateId()); // confirmed
        assertFalse(keyMapper.has(new Item("Item 0")));
        assertFalse(keyMapper.has(new Item("Item 0-0")));
        assertTrue(keyMapper.has(new Item("Item 0-1")));
        assertTrue(keyMapper.has(new Item("Item 1")));
        assertTrue(keyMapper.has(new Item("Item 2")));
        assertTrue(keyMapper.has(new Item("Item 3")));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication(); // not confirmed yet
        assertTrue(keyMapper.has(new Item("Item 2")));
        assertTrue(keyMapper.has(new Item("Item 3")));

        dataCommunicator.confirmUpdate(captureArrayUpdateId()); // confirmed
        assertTrue(keyMapper.has(new Item("Item 0")));
        assertTrue(keyMapper.has(new Item("Item 0-0")));
        assertTrue(keyMapper.has(new Item("Item 0-1")));
        assertTrue(keyMapper.has(new Item("Item 1")));
        assertFalse(keyMapper.has(new Item("Item 2")));
        assertFalse(keyMapper.has(new Item("Item 3")));
    }

    @Test
    void collapseItems_collapsedKeysRemoved() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.expand(new Item("Item 0"));
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();
        assertTrue(keyMapper.has(new Item("Item 0-0")));
        assertTrue(keyMapper.has(new Item("Item 0-1")));

        dataCommunicator.collapse(new Item("Item 0"));
        fakeClientCommunication();
        assertFalse(keyMapper.has(new Item("Item 0-0")));
        assertFalse(keyMapper.has(new Item("Item 0-1")));
    }

    @Test
    void setUniqueKeyProvider_keysGeneratedByProvider() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.expand(new Item("Item 0"));
        dataCommunicator.setViewportRange(0, 4);

        uniqueKeyProvider = (item) -> {
            return "key-" + item.getName().toLowerCase().replace("item ", "");
        };
        fakeClientCommunication();

        assertArrayUpdateItems("key", "key-0", "key-0-0", "key-0-1", "key-1");
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 0")),
                keyMapper.key(new Item("Item 0-0")),
                keyMapper.key(new Item("Item 0-1")),
                keyMapper.key(new Item("Item 1")));
    }

    @Test
    void generateKeyManually_resetBeforeInitFlush_keyPreserved() {
        populateTreeData(treeData, 100, 2, 2);

        var key = keyMapper.key(new Item("Item 4"));
        dataCommunicator.reset();
        assertEquals(new Item("Item 4"), keyMapper.get(key));
    }
}
