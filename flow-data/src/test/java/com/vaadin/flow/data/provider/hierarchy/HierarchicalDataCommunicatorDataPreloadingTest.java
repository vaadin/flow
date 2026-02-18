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
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.provider.CompositeDataGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HierarchicalDataCommunicatorDataPreloadingTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @BeforeEach
    void init() {
        super.init();

        var treeData = new TreeData<Item>();
        populateTreeData(treeData, 20, 2, 2);

        dataCommunicator = new HierarchicalDataCommunicator<>(
                new CompositeDataGenerator<Item>(), arrayUpdater,
                ui.getElement().getNode(), () -> null);
        dataCommunicator.setDataProvider(new TreeDataProvider<>(treeData),
                null);
    }

    @Test
    void preloadFlatRangeForward_nearStart_requestedRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 2"), new Item("Item 2-0"), new Item("Item 1-0"),
                new Item("Item 9")));

        var items = dataCommunicator.preloadFlatRangeForward(0, 10);
        assertPreloadedRange(
                "Item 0, Item 0-0, Item 0-1, Item 1, Item 2, Item 2-0, Item 2-0-0, Item 2-0-1, Item 2-1, Item 3",
                items);

        assertEquals(26, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void preloadFlatRangeForward_nearMiddle_requestedRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeForward(9, 6);
        assertPreloadedRange(
                "Item 9, Item 9-0, Item 9-1, Item 10, Item 11, Item 12", items);

        assertEquals(22, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void preloadFlatRangeForward_nearEnd_requestedRangeReturned() {
        dataCommunicator
                .expand(Arrays.asList(new Item("Item 0"), new Item("Item 9"),
                        new Item("Item 19"), new Item("Item 19-1")));

        var items = dataCommunicator.preloadFlatRangeForward(18, 6);
        assertPreloadedRange(
                "Item 18, Item 19, Item 19-0, Item 19-1, Item 19-1-0, Item 19-1-1",
                items);

        assertEquals(24, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void preloadFlatRangeForward_rangeEndOutOfBounds_availableRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeForward(19, 10);
        assertPreloadedRange("Item 19, Item 19-0, Item 19-1", items);

        assertEquals(22, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void preloadFlatRangeForward_rangeStartOutOfBounds_emptyRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeForward(100, 10);
        assertEquals(0, items.size());

        assertEquals(20, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void preloadFlatRangeBackward_nearEnd_requestedRangeReturned() {
        dataCommunicator
                .expand(Arrays.asList(new Item("Item 19"), new Item("Item 18"),
                        new Item("Item 18-1"), new Item("Item 17-1"),
                        new Item("Item 16"), new Item("Item 9")));

        var items = dataCommunicator.preloadFlatRangeBackward(19, 10);
        assertPreloadedRange(
                "Item 16, Item 16-0, Item 16-1, Item 17, Item 18, Item 18-0, Item 18-1, Item 18-1-0, Item 18-1-1, Item 19",
                items);

        assertEquals(26, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void preloadFlatRangeBackward_nearMiddle_requestedRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 12"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeBackward(12, 6);
        assertPreloadedRange(
                "Item 9, Item 9-0, Item 9-1, Item 10, Item 11, Item 12", items);

        assertEquals(22, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void preloadFlatRangeBackward_nearStart_requestedRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 0-1"), new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeBackward(1, 6);
        assertPreloadedRange(
                "Item 0, Item 0-0, Item 0-1, Item 0-1-0, Item 0-1-1, Item 1",
                items);

        assertEquals(24, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void preloadFlatRangeBackward_rangeEndOutOfBounds_availableRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeBackward(0, 10);
        assertPreloadedRange("Item 0", items);

        assertEquals(20, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    void preloadFlatRangeBackward_rangeStartOutOfBounds_emptyRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));
        var items = dataCommunicator.preloadFlatRangeBackward(-100, 10);
        assertEquals(0, items.size());

        assertEquals(20, dataCommunicator.rootCache.getFlatSize());
    };

    private void assertPreloadedRange(String expectedItems, List<Item> items) {
        assertEquals(expectedItems, items.stream().map(Item::getName)
                .collect(Collectors.joining(", ")));
    }
}
