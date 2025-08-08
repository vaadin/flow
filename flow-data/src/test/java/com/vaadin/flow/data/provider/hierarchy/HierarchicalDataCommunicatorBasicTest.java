package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.dom.Element;

import elemental.json.JsonObject;

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

        Element element = new Element("div");

        dataCommunicator = new HierarchicalDataCommunicator<>(
                compositeDataGenerator, arrayUpdater, (items) -> {
                }, element.getNode(), () -> null);

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
        Assert.assertEquals(-1,
                dataCommunicator.getDepth(new Item("NOT EXISTING")));

        fakeClientCommunication();

        Assert.assertEquals(0, dataCommunicator.getDepth(new Item("Item 0")));
        Assert.assertEquals(1, dataCommunicator.getDepth(new Item("Item 0-0")));
        Assert.assertEquals(2,
                dataCommunicator.getDepth(new Item("Item 0-0-0")));
        Assert.assertEquals(0, dataCommunicator.getDepth(new Item("Item 1")));
        Assert.assertEquals(-1,
                dataCommunicator.getDepth(new Item("NOT EXISTING")));

        dataCommunicator.setViewportRange(4, 4);
        fakeClientCommunication();

        Assert.assertEquals(1, dataCommunicator.getDepth(new Item("Item 0-0")));
        Assert.assertEquals(0, dataCommunicator.getDepth(new Item("Item 5")));
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
            public void generateData(Item item, JsonObject json) {
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
}
