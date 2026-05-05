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

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.data.provider.CompositeDataGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HierarchicalDataCommunicatorIndexPathResolutionTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @BeforeEach
    void init() {
        super.init();

        var treeData = new TreeData<Item>();
        populateTreeData(treeData, 3, 2, 1);

        var dataGenerator = new CompositeDataGenerator<Item>();
        dataGenerator.addDataGenerator(
                (item, json) -> json.put("name", item.getName()));

        dataCommunicator = new HierarchicalDataCommunicator<>(dataGenerator,
                arrayUpdater, ui.getElement().getNode(), () -> null);
        dataCommunicator.setDataProvider(new TreeDataProvider<>(treeData),
                null);
    }

    @Test
    void positiveIndexes_resolveIndexPath_correctFlatIndexReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 1"), new Item("Item 1-1")));

        assertEquals(0, dataCommunicator.resolveIndexPath(0));
        assertEquals(3, dataCommunicator.rootCache.getFlatSize());

        assertEquals(1, dataCommunicator.resolveIndexPath(0, 0));
        assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        assertEquals(3, dataCommunicator.resolveIndexPath(1));
        assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        assertEquals(4, dataCommunicator.resolveIndexPath(1, 0));
        assertEquals(7, dataCommunicator.rootCache.getFlatSize());

        assertEquals(5, dataCommunicator.resolveIndexPath(1, 1));
        assertEquals(7, dataCommunicator.rootCache.getFlatSize());

        assertEquals(6, dataCommunicator.resolveIndexPath(1, 1, 0));
        assertEquals(8, dataCommunicator.rootCache.getFlatSize());

        assertEquals(7, dataCommunicator.resolveIndexPath(2));
        assertEquals(8, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void negativeIndexes_resolveIndexPath_correctFlatIndexReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 1"), new Item("Item 1-1")));

        assertEquals(0, dataCommunicator.resolveIndexPath(-3));
        assertEquals(3, dataCommunicator.rootCache.getFlatSize());

        assertEquals(1, dataCommunicator.resolveIndexPath(-3, -2));
        assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        assertEquals(3, dataCommunicator.resolveIndexPath(-2));
        assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        assertEquals(4, dataCommunicator.resolveIndexPath(-2, -2));
        assertEquals(7, dataCommunicator.rootCache.getFlatSize());

        assertEquals(5, dataCommunicator.resolveIndexPath(-2, -1));
        assertEquals(7, dataCommunicator.rootCache.getFlatSize());

        assertEquals(6, dataCommunicator.resolveIndexPath(-2, -1, -1));
        assertEquals(8, dataCommunicator.rootCache.getFlatSize());

        assertEquals(7, dataCommunicator.resolveIndexPath(-1));
        assertEquals(8, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void exceedingPositiveIndexes_resolveIndexPath_indexesClamped() {
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 2"), new Item("Item 2-1")));

        assertEquals(2, dataCommunicator.resolveIndexPath(100));
        assertEquals(3, dataCommunicator.rootCache.getFlatSize());

        assertEquals(4, dataCommunicator.resolveIndexPath(100, 100));
        assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        assertEquals(5, dataCommunicator.resolveIndexPath(100, 100, 100));
        assertEquals(6, dataCommunicator.rootCache.getFlatSize());

        assertEquals(5, dataCommunicator.resolveIndexPath(100, 100, 100, 100));
        assertEquals(6, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void exceedingNegativeIndexes_resolveIndexPath_indexesClamped() {
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));

        assertEquals(0, dataCommunicator.resolveIndexPath(-100));
        assertEquals(3, dataCommunicator.rootCache.getFlatSize());

        assertEquals(1, dataCommunicator.resolveIndexPath(-100, -100));
        assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        assertEquals(2, dataCommunicator.resolveIndexPath(-100, -100, -100));
        assertEquals(6, dataCommunicator.rootCache.getFlatSize());

        assertEquals(2,
                dataCommunicator.resolveIndexPath(-100, -100, -100, -100));
        assertEquals(6, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void setViewportRange_resolveIndexPage_entireRangeSentWhenSizeChanged() {
        dataCommunicator.expand(new Item("Item 0"));
        dataCommunicator.setViewportRange(2, 1);
        fakeClientCommunication();
        assertArrayUpdateSize(3);
        assertArrayUpdateRange(2, 1);
        assertArrayUpdateItems("name", Map.of(2, "Item 2"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.resolveIndexPath(0);
        fakeClientCommunication();
        Mockito.verifyNoInteractions(arrayUpdater, arrayUpdate);

        dataCommunicator.resolveIndexPath(0, 0);
        fakeClientCommunication();
        assertArrayUpdateSize(5);
        assertArrayUpdateRange(2, 1);
        assertArrayUpdateItems("name", Map.of(2, "Item 0-1"));
    }
}
