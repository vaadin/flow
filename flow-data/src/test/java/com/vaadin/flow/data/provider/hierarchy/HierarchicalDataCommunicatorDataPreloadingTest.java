package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.data.provider.CompositeDataGenerator;

public class HierarchicalDataCommunicatorDataPreloadingTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @Before
    public void init() {
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
    public void preloadFlatRangeForward_nearStart_requestedRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 2"), new Item("Item 2-0"), new Item("Item 1-0"),
                new Item("Item 9")));

        var items = dataCommunicator.preloadFlatRangeForward(0, 10);
        assertPreloadedRange(
                "Item 0, Item 0-0, Item 0-1, Item 1, Item 2, Item 2-0, Item 2-0-0, Item 2-0-1, Item 2-1, Item 3",
                items);

        Assert.assertEquals(26, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void preloadFlatRangeForward_nearMiddle_requestedRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeForward(9, 6);
        assertPreloadedRange(
                "Item 9, Item 9-0, Item 9-1, Item 10, Item 11, Item 12", items);

        Assert.assertEquals(22, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void preloadFlatRangeForward_nearEnd_requestedRangeReturned() {
        dataCommunicator
                .expand(Arrays.asList(new Item("Item 0"), new Item("Item 9"),
                        new Item("Item 19"), new Item("Item 19-1")));

        var items = dataCommunicator.preloadFlatRangeForward(18, 6);
        assertPreloadedRange(
                "Item 18, Item 19, Item 19-0, Item 19-1, Item 19-1-0, Item 19-1-1",
                items);

        Assert.assertEquals(24, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void preloadFlatRangeForward_rangeEndOutOfBounds_availableRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeForward(19, 10);
        assertPreloadedRange("Item 19, Item 19-0, Item 19-1", items);

        Assert.assertEquals(22, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void preloadFlatRangeForward_rangeStartOutOfBounds_emptyRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeForward(100, 10);
        Assert.assertEquals(0, items.size());

        Assert.assertEquals(20, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void preloadFlatRangeBackward_nearEnd_requestedRangeReturned() {
        dataCommunicator
                .expand(Arrays.asList(new Item("Item 19"), new Item("Item 18"),
                        new Item("Item 18-1"), new Item("Item 17-1"),
                        new Item("Item 16"), new Item("Item 9")));

        var items = dataCommunicator.preloadFlatRangeBackward(19, 10);
        assertPreloadedRange(
                "Item 16, Item 16-0, Item 16-1, Item 17, Item 18, Item 18-0, Item 18-1, Item 18-1-0, Item 18-1-1, Item 19",
                items);

        Assert.assertEquals(26, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void preloadFlatRangeBackward_nearMiddle_requestedRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 12"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeBackward(12, 6);
        assertPreloadedRange(
                "Item 9, Item 9-0, Item 9-1, Item 10, Item 11, Item 12", items);

        Assert.assertEquals(22, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void preloadFlatRangeBackward_nearStart_requestedRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 0-1"), new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeBackward(1, 6);
        assertPreloadedRange(
                "Item 0, Item 0-0, Item 0-1, Item 0-1-0, Item 0-1-1, Item 1",
                items);

        Assert.assertEquals(24, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void preloadFlatRangeBackward_rangeEndOutOfBounds_availableRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));

        var items = dataCommunicator.preloadFlatRangeBackward(0, 10);
        assertPreloadedRange("Item 0", items);

        Assert.assertEquals(20, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void preloadFlatRangeBackward_rangeStartOutOfBounds_emptyRangeReturned() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 9"), new Item("Item 19")));
        var items = dataCommunicator.preloadFlatRangeBackward(-100, 10);
        Assert.assertEquals(0, items.size());

        Assert.assertEquals(20, dataCommunicator.rootCache.getFlatSize());
    };

    private void assertPreloadedRange(String expectedItems, List<Item> items) {
        Assert.assertEquals(expectedItems, items.stream().map(Item::getName)
                .collect(Collectors.joining(", ")));
    }
}
