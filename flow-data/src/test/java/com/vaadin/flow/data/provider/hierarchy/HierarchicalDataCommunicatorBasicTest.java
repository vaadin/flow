package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.provider.QuerySortOrderBuilder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.dom.Element;

public class HierarchicalDataCommunicatorBasicTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    private CompositeDataGenerator<Item> compositeDataGenerator = new CompositeDataGenerator<>();
    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @Before
    public void init() {
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
    public void flush_emptyRangeSent() {
        fakeClientCommunication();
        assertArrayUpdateSize(0);
        assertArrayUpdateRange(0, 0);
    }

    @Test
    public void setViewportRange_flush_emptyRangeSent() {
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();
        assertArrayUpdateSize(0);
        assertArrayUpdateRange(0, 0);
    }

    @Test
    public void flush_arrayUpdaterInitializedOnce() {
        fakeClientCommunication();
        Mockito.verify(arrayUpdater, Mockito.times(1)).initialize();

        Mockito.clearInvocations(arrayUpdater);

        fakeClientCommunication();
        Mockito.verify(arrayUpdater, Mockito.never()).initialize();
    }

    @Test
    public void setViewportRange_flush_arrayUpdaterInitializedOnce() {
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();
        Mockito.verify(arrayUpdater, Mockito.times(1)).initialize();

        Mockito.clearInvocations(arrayUpdater);

        dataCommunicator.setViewportRange(50, 50);
        fakeClientCommunication();
        Mockito.verify(arrayUpdater, Mockito.never()).initialize();
    }

    @Test
    public void setDataProvider_getDataProvider() {
        dataCommunicator.setDataProvider(treeDataProvider, null);
        Assert.assertEquals(treeDataProvider,
                dataCommunicator.getDataProvider());
    }

    @Test
    public void setDataProvider_expandItem_setAnotherDataProvider_expandedItemsCleared() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setDataProvider(new TreeDataProvider<Item>(treeData),
                null);
        dataCommunicator.expand(new Item("Item 0"));
        Assert.assertTrue(dataCommunicator.isExpanded(new Item("Item 0")));

        dataCommunicator.setDataProvider(new TreeDataProvider<Item>(treeData),
                null);
        Assert.assertFalse(dataCommunicator.isExpanded(new Item("Item 0")));
    }

    @Test
    public void setIncompatibleDataProvider_throws() {
        ListDataProvider<Item> incompatibleDataProvider = DataProvider
                .ofItems(new Item("Item 0"));

        Assert.assertThrows(IllegalArgumentException.class,
                () -> dataCommunicator.setDataProvider(incompatibleDataProvider,
                        null));
    }

    @Test
    public void setDataProviderWithNullItems_setViewport_throws() {
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

        Assert.assertThrows(IllegalStateException.class,
                () -> fakeClientCommunication());
    }

    @Test
    public void setDataProviderWithNegativeChildCount_setViewport_throws() {
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

        Assert.assertThrows(IllegalStateException.class,
                () -> fakeClientCommunication());
    }

    @Test
    public void getDepth_returnsDepthForCachedItemsAfterTheyAreLoaded() {
        populateTreeData(treeData, 100, 1, 1);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 4);

        Assert.assertEquals(-1, dataCommunicator.getDepth(new Item("Item 0")));
        Assert.assertEquals(-1,
                dataCommunicator.getDepth(new Item("Item 0-0")));
        Assert.assertEquals(-1,
                dataCommunicator.getDepth(new Item("Item 0-0-0")));
        Assert.assertEquals(-1, dataCommunicator.getDepth(new Item("Item 1")));

        fakeClientCommunication();

        Assert.assertEquals(0, dataCommunicator.getDepth(new Item("Item 0")));
        Assert.assertEquals(1, dataCommunicator.getDepth(new Item("Item 0-0")));
        Assert.assertEquals(2,
                dataCommunicator.getDepth(new Item("Item 0-0-0")));
        Assert.assertEquals(0, dataCommunicator.getDepth(new Item("Item 1")));

        dataCommunicator.setViewportRange(4, 4);
        fakeClientCommunication();

        Assert.assertEquals(1, dataCommunicator.getDepth(new Item("Item 0-0")));
        Assert.assertEquals(0, dataCommunicator.getDepth(new Item("Item 5")));
    }

    @Test
    public void getDepth_doesNotReturnDepthForNonExistingItems() {
        populateTreeData(treeData, 100, 1, 1);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();

        Assert.assertEquals(-1,
                dataCommunicator.getDepth(new Item("NOT EXISTING")));
    }

    @Test
    public void hasChildren_returnsValueBasedOnDataProvider() {
        dataCommunicator.setDataProvider(treeDataProvider, null);

        populateTreeData(treeData, 1, 1);
        Assert.assertTrue(dataCommunicator.hasChildren(new Item("Item 0")));
        Assert.assertFalse(dataCommunicator.hasChildren(new Item("Item 0-0")));

        populateTreeData(treeData, 1);
        Assert.assertFalse(dataCommunicator.hasChildren(new Item("Item 0")));
        Assert.assertFalse(dataCommunicator.hasChildren(new Item("Item 0-0")));
    }

    @Test
    public void expandCollectionOfItems_returnsEffectivelyExpandedItems() {
        populateTreeData(treeData, 4, 1, 1);
        dataCommunicator.setDataProvider(treeDataProvider, null);

        Assert.assertEquals(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")),
                dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                        new Item("Item 0-0"), new Item("Item 0-0-0"))));

        Assert.assertEquals(Arrays.asList(new Item("Item 1")),
                dataCommunicator.expand(Arrays.asList(new Item("Item 0-0"),
                        new Item("Item 1"))));
    }

    @Test
    public void collapseCollectionOfItems_returnsEffectivelyCollapsedItems() {
        populateTreeData(treeData, 4, 1, 1);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));

        Assert.assertEquals(Arrays.asList(new Item("Item 0")),
                dataCommunicator.collapse(
                        Arrays.asList(new Item("Item 0"), new Item("Item 1"))));
    }

    @Test
    public void collapseItems_collapsedChildrenRemovedFromKeyMapper() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.expand(new Item("Item 0"));
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();
        Assert.assertTrue(
                dataCommunicator.getKeyMapper().has(new Item("Item 0-0")));
        Assert.assertTrue(
                dataCommunicator.getKeyMapper().has(new Item("Item 0-1")));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0"));
        fakeClientCommunication();
        Assert.assertFalse(
                dataCommunicator.getKeyMapper().has(new Item("Item 0-0")));
        Assert.assertFalse(
                dataCommunicator.getKeyMapper().has(new Item("Item 0-1")));
    }

    @Test
    public void collapseItems_dataGeneratorDestroyDataCalledForCollapsedChildren() {
        populateTreeData(treeData, 4, 2, 2);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();

        var dataGenerator = Mockito.spy(new DataGenerator<Item>() {
            @Override
            public void generateData(Item item, ObjectNode json) {
                // NO-OP
            }
        });
        compositeDataGenerator.addDataGenerator(dataGenerator);

        dataCommunicator.collapse(new Item("Item 0"));

        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 0"));
        Mockito.verify(dataGenerator).destroyData(new Item("Item 0-0"));
        Mockito.verify(dataGenerator).destroyData(new Item("Item 0-1"));
        Mockito.verify(dataGenerator).destroyData(new Item("Item 0-0-0"));
        Mockito.verify(dataGenerator).destroyData(new Item("Item 0-0-1"));
    }

    @Test
    public void setFilterViaDataProvider_filterApplied() {
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
    public void setFilterViaCallback_filterApplied() {
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
    public void setInMemorySorting_sortingApplied() {
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
    public void setBackendSorting_sortingApplied() {
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
}
