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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.QuerySortOrderBuilder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchicalDataCommunicatorBasicTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    private CompositeDataGenerator<Item> compositeDataGenerator = new CompositeDataGenerator<>();
    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @BeforeEach
    void init() {
        super.init();

        var element = new Element("div");

        dataCommunicator = new HierarchicalDataCommunicator<>(
                compositeDataGenerator, arrayUpdater, element.getNode(),
                () -> null);

        compositeDataGenerator.addDataGenerator((item, json) -> {
            json.put("name", item.getName());
        });

        ui.getElement().appendChild(element);
    }

    @Test
    void flush_emptyRangeSent() {
        fakeClientCommunication();
        assertArrayUpdateSize(0);
        assertArrayUpdateRange(0, 0);
    }

    @Test
    void setViewportRange_flush_emptyRangeSent() {
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();
        assertArrayUpdateSize(0);
        assertArrayUpdateRange(0, 0);
    }

    @Test
    void flush_arrayUpdaterInitializedOnce() {
        fakeClientCommunication();
        Mockito.verify(arrayUpdater).initialize();

        Mockito.clearInvocations(arrayUpdater);

        fakeClientCommunication();
        Mockito.verify(arrayUpdater, Mockito.never()).initialize();
    }

    @Test
    void setViewportRange_flush_arrayUpdaterInitializedOnce() {
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();
        Mockito.verify(arrayUpdater).initialize();

        Mockito.clearInvocations(arrayUpdater);

        dataCommunicator.setViewportRange(50, 50);
        fakeClientCommunication();
        Mockito.verify(arrayUpdater, Mockito.never()).initialize();
    }

    @Test
    void setDataProvider_getDataProvider() {
        dataCommunicator.setDataProvider(treeDataProvider, null);
        assertEquals(treeDataProvider, dataCommunicator.getDataProvider());
    }

    @Test
    void setDataProvider_expandItem_setAnotherDataProvider_expandedItemsCleared() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setDataProvider(new TreeDataProvider<Item>(treeData),
                null);
        dataCommunicator.expand(new Item("Item 0"));
        assertTrue(dataCommunicator.isExpanded(new Item("Item 0")));

        dataCommunicator.setDataProvider(new TreeDataProvider<Item>(treeData),
                null);
        assertFalse(dataCommunicator.isExpanded(new Item("Item 0")));
    }

    @Test
    void setIncompatibleDataProvider_throws() {
        ListDataProvider<Item> incompatibleDataProvider = DataProvider
                .ofItems(new Item("Item 0"));

        assertThrows(IllegalArgumentException.class, () -> dataCommunicator
                .setDataProvider(incompatibleDataProvider, null));
    }

    @Test
    void setDataProviderWithNullItems_setViewport_throws() {
        dataCommunicator.setDataProvider(
                new AbstractBackEndHierarchicalDataProvider<Item, Void>() {
                    @Override
                    public Stream<Item> fetchChildrenFromBackEnd(
                            HierarchicalQuery<Item, Void> query) {
                        return Stream.of(null, null, null);
                    }

                    @Override
                    public int getChildCount(
                            HierarchicalQuery<Item, Void> query) {
                        return 3;
                    }

                    @Override
                    public boolean hasChildren(Item item) {
                        return false;
                    }
                }, null);
        dataCommunicator.setViewportRange(0, 3);

        assertThrows(IllegalStateException.class,
                () -> fakeClientCommunication());
    }

    @Test
    void setDataProviderWithNegativeChildCount_setViewport_throws() {
        dataCommunicator.setDataProvider(
                new AbstractBackEndHierarchicalDataProvider<Item, Void>() {
                    @Override
                    public Stream<Item> fetchChildrenFromBackEnd(
                            HierarchicalQuery<Item, Void> query) {
                        return Stream.of(new Item("Item 0"), new Item("Item 1"),
                                new Item("Item 2"));
                    }

                    @Override
                    public int getChildCount(
                            HierarchicalQuery<Item, Void> query) {
                        return -3;
                    }

                    @Override
                    public boolean hasChildren(Item item) {
                        return false;
                    }
                }, null);
        dataCommunicator.setViewportRange(0, 3);

        assertThrows(IllegalStateException.class,
                () -> fakeClientCommunication());
    }

    @Test
    void getDepth_returnsDepthForViewportItems() {
        populateTreeData(treeData, 100, 1, 1);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 4);

        // Not loaded yet
        assertEquals(-1, dataCommunicator.getDepth(new Item("Item 0")));
        assertEquals(-1, dataCommunicator.getDepth(new Item("Item 0-0")));
        assertEquals(-1, dataCommunicator.getDepth(new Item("Item 0-0-0")));
        assertEquals(-1, dataCommunicator.getDepth(new Item("Item 1")));

        fakeClientCommunication();
        assertEquals(0, dataCommunicator.getDepth(new Item("Item 0")));
        assertEquals(1, dataCommunicator.getDepth(new Item("Item 0-0")));
        assertEquals(2, dataCommunicator.getDepth(new Item("Item 0-0-0")));
        assertEquals(0, dataCommunicator.getDepth(new Item("Item 1")));

        dataCommunicator.setViewportRange(4, 4);
        fakeClientCommunication();
        assertEquals(0, dataCommunicator.getDepth(new Item("Item 5")));
    }

    @Test
    void getDepth_doesNotReturnDepthForNonExistingItems() {
        populateTreeData(treeData, 100, 1, 1);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();

        assertEquals(-1, dataCommunicator.getDepth(new Item("NOT EXISTING")));
    }

    @Test
    void hasChildren_returnsValueBasedOnDataProvider() {
        dataCommunicator.setDataProvider(treeDataProvider, null);

        populateTreeData(treeData, 1, 1);
        assertTrue(dataCommunicator.hasChildren(new Item("Item 0")));
        assertFalse(dataCommunicator.hasChildren(new Item("Item 0-0")));

        populateTreeData(treeData, 1);
        assertFalse(dataCommunicator.hasChildren(new Item("Item 0")));
        assertFalse(dataCommunicator.hasChildren(new Item("Item 0-0")));
    }

    @Test
    void expandCollectionOfItems_returnsEffectivelyExpandedItems() {
        populateTreeData(treeData, 4, 1, 1);
        dataCommunicator.setDataProvider(treeDataProvider, null);

        assertEquals(Arrays.asList(new Item("Item 0"), new Item("Item 0-0")),
                dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                        new Item("Item 0-0"), new Item("Item 0-0-0"))));

        assertEquals(Arrays.asList(new Item("Item 1")), dataCommunicator.expand(
                Arrays.asList(new Item("Item 0-0"), new Item("Item 1"))));
    }

    @Test
    void collapseCollectionOfItems_returnsEffectivelyCollapsedItems() {
        populateTreeData(treeData, 4, 1, 1);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));

        assertEquals(Arrays.asList(new Item("Item 0")),
                dataCommunicator.collapse(
                        Arrays.asList(new Item("Item 0"), new Item("Item 1"))));
    }

    @Test
    void setFilterViaDataProvider_filterApplied() {
        populateTreeData(treeData, 3, 2, 1);
        dataCommunicator.setDataProvider(treeDataProvider,
                (item) -> item.equals(new Item("Item 1"))
                        || item.equals(new Item("Item 1-1")));
        dataCommunicator.expand(new Item("Item 1"));
        dataCommunicator.setViewportRange(0, 10);
        fakeClientCommunication();

        assertArrayUpdateSize(2);
        assertArrayUpdateItems("name", "Item 1", "Item 1-1");

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setDataProvider(treeDataProvider,
                (item) -> item.equals(new Item("Item 0")));
        fakeClientCommunication();

        assertArrayUpdateSize(1);
        assertArrayUpdateItems("name", "Item 0");
    }

    @Test
    void setFilterViaCallback_filterApplied() {
        populateTreeData(treeData, 3, 2, 1);
        var filterCallback = dataCommunicator.setDataProvider(treeDataProvider,
                null);
        dataCommunicator.expand(new Item("Item 1"));
        dataCommunicator.setViewportRange(0, 10);

        filterCallback.accept(item -> item.equals(new Item("Item 1"))
                || item.equals(new Item("Item 1-1")));
        fakeClientCommunication();

        assertArrayUpdateSize(2);
        assertArrayUpdateItems("name", "Item 1", "Item 1-1");

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        filterCallback.accept(item -> item.equals(new Item("Item 0")));
        fakeClientCommunication();

        assertArrayUpdateSize(1);
        assertArrayUpdateItems("name", "Item 0");
    }

    @Test
    void setInMemorySorting_sortingApplied() {
        populateTreeData(treeData, 3, 2, 1);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.expand(new Item("Item 1"));
        dataCommunicator.setViewportRange(0, 10);

        dataCommunicator.setInMemorySorting(
                Comparator.comparing(Item::getName).reversed()::compare);
        fakeClientCommunication();

        assertArrayUpdateSize(5);
        assertArrayUpdateItems("name", "Item 2", "Item 1", "Item 1-1",
                "Item 1-0", "Item 0");

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setInMemorySorting(null);
        fakeClientCommunication();

        assertArrayUpdateSize(5);
        assertArrayUpdateItems("name", "Item 0", "Item 1", "Item 1-0",
                "Item 1-1", "Item 2");
    }

    @Test
    void setBackendSorting_sortingApplied() {
        populateTreeData(treeData, 3, 2, 1);
        dataCommunicator.setDataProvider(
                new AbstractBackEndHierarchicalDataProvider<Item, Void>() {
                    @Override
                    public Stream<Item> fetchChildrenFromBackEnd(
                            HierarchicalQuery<Item, Void> query) {
                        Comparator<Item> sortComparator = null;

                        if (!query.getSortOrders().isEmpty()) {
                            var sortOrder = query.getSortOrders().get(0);
                            if (sortOrder.getSorted().equals("name")) {
                                sortComparator = Comparator
                                        .comparing(Item::getName);
                            }
                            if (sortOrder.getDirection()
                                    .equals(SortDirection.DESCENDING)) {
                                sortComparator = sortComparator.reversed();
                            }
                        }

                        var items = treeData.getChildren(query.getParent())
                                .stream();
                        if (sortComparator != null) {
                            items = items.sorted(sortComparator);
                        }
                        return items;
                    }

                    @Override
                    public int getChildCount(
                            HierarchicalQuery<Item, Void> query) {
                        return treeData.getChildren(query.getParent()).size();
                    }

                    @Override
                    public boolean hasChildren(Item item) {
                        return !treeData.getChildren(item).isEmpty();
                    }
                }, null);
        dataCommunicator.expand(new Item("Item 1"));
        dataCommunicator.setViewportRange(0, 10);

        dataCommunicator.setBackEndSorting(
                new QuerySortOrderBuilder().thenDesc("name").build());
        fakeClientCommunication();

        assertArrayUpdateSize(5);
        assertArrayUpdateItems("name", "Item 2", "Item 1", "Item 1-1",
                "Item 1-0", "Item 0");

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setBackEndSorting(Collections.emptyList());
        fakeClientCommunication();

        assertArrayUpdateSize(5);
        assertArrayUpdateItems("name", "Item 0", "Item 1", "Item 1-0",
                "Item 1-1", "Item 2");
    }

    @Test
    void buildQuery_correctQueryReturned() {
        List<QuerySortOrder> sortOrders = new QuerySortOrderBuilder()
                .thenDesc("name").build();
        SerializablePredicate<Item> filter = (f) -> true;
        SerializableComparator<Item> comparator = Comparator
                .comparing(Item::getName).reversed()::compare;

        dataCommunicator.setDataProvider(treeDataProvider, filter);
        dataCommunicator.setInMemorySorting(comparator);
        dataCommunicator.setBackEndSorting(sortOrders);

        var query = dataCommunicator.buildQuery(10, 20);
        assertNull(query.getParent());
        assertEquals(10, query.getOffset());
        assertEquals(20, query.getLimit());
        assertEquals(filter, query.getFilter().get());
        assertEquals(sortOrders, query.getSortOrders());
        assertEquals(comparator, query.getInMemorySorting());
        assertEquals(Collections.emptySet(), query.getExpandedItemIds());

        query = dataCommunicator.buildQuery(new Item("Parent"), 10, 20);
        assertEquals(new Item("Parent"), query.getParent());
        assertEquals(10, query.getOffset());
        assertEquals(20, query.getLimit());
        assertEquals(filter, query.getFilter().get());
        assertEquals(sortOrders, query.getSortOrders());
        assertEquals(comparator, query.getInMemorySorting());
        assertEquals(Collections.emptySet(), query.getExpandedItemIds());
    }
}
