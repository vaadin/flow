package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.data.provider.CompositeDataGenerator;

public class HierarchicalDataCommunicatorIndexPathResolutionTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @Before
    public void init() {
        super.init();

        var treeData = new TreeData<Item>();
        populateTreeData(treeData, 3, 2, 1);

        dataCommunicator = new HierarchicalDataCommunicator<>(
                new CompositeDataGenerator<Item>(), arrayUpdater, (items) -> {
                }, ui.getElement().getNode(), () -> null);
        dataCommunicator.setDataProvider(new TreeDataProvider<>(treeData),
                null);
    }

    @Test
    public void positiveIndexes_resolveIndexPath_correctFlatIndexReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 1"), new Item("Item 1-1")));

        Assert.assertEquals(0, dataCommunicator.resolveIndexPath(0));
        Assert.assertEquals(3, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(1, dataCommunicator.resolveIndexPath(0, 0));
        Assert.assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(3, dataCommunicator.resolveIndexPath(1));
        Assert.assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(4, dataCommunicator.resolveIndexPath(1, 0));
        Assert.assertEquals(7, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(5, dataCommunicator.resolveIndexPath(1, 1));
        Assert.assertEquals(7, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(6, dataCommunicator.resolveIndexPath(1, 1, 0));
        Assert.assertEquals(8, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(7, dataCommunicator.resolveIndexPath(2));
        Assert.assertEquals(8, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void negativeIndexes_resolveIndexPath_correctFlatIndexReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 1"), new Item("Item 1-1")));

        Assert.assertEquals(0, dataCommunicator.resolveIndexPath(-3));
        Assert.assertEquals(3, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(1, dataCommunicator.resolveIndexPath(-3, -2));
        Assert.assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(3, dataCommunicator.resolveIndexPath(-2));
        Assert.assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(4, dataCommunicator.resolveIndexPath(-2, -2));
        Assert.assertEquals(7, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(5, dataCommunicator.resolveIndexPath(-2, -1));
        Assert.assertEquals(7, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(6, dataCommunicator.resolveIndexPath(-2, -1, -1));
        Assert.assertEquals(8, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(7, dataCommunicator.resolveIndexPath(-1));
        Assert.assertEquals(8, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void exceedingPositiveIndexes_resolveIndexPath_indexesClamped() {
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 2"), new Item("Item 2-1")));

        Assert.assertEquals(2, dataCommunicator.resolveIndexPath(100));
        Assert.assertEquals(3, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(4, dataCommunicator.resolveIndexPath(100, 100));
        Assert.assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(5,
                dataCommunicator.resolveIndexPath(100, 100, 100));
        Assert.assertEquals(6, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(5,
                dataCommunicator.resolveIndexPath(100, 100, 100, 100));
        Assert.assertEquals(6, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void exceedingNegativeIndexes_resolveIndexPath_indexesClamped() {
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));

        Assert.assertEquals(0, dataCommunicator.resolveIndexPath(-100));
        Assert.assertEquals(3, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(1, dataCommunicator.resolveIndexPath(-100, -100));
        Assert.assertEquals(5, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(2,
                dataCommunicator.resolveIndexPath(-100, -100, -100));
        Assert.assertEquals(6, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(2,
                dataCommunicator.resolveIndexPath(-100, -100, -100, -100));
        Assert.assertEquals(6, dataCommunicator.rootCache.getFlatSize());
    }
}
