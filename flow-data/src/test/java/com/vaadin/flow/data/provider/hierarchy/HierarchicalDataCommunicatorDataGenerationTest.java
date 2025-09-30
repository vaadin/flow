package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataGenerator;

public class HierarchicalDataCommunicatorDataGenerationTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @Mock
    private DataGenerator<Item> dataGenerator;

    @Before
    public void init() {
        super.init();

        var compositeDataGenerator = new CompositeDataGenerator<Item>();

        dataCommunicator = new HierarchicalDataCommunicator<>(
                compositeDataGenerator, arrayUpdater, ui.getElement().getNode(),
                () -> null);
        dataCommunicator.setDataProvider(treeDataProvider, null);

        compositeDataGenerator.addDataGenerator(dataGenerator);
    }

    @Test
    public void changeViewportRangeBackAndForth_generateDataCalledForVisibleItems() {
        populateTreeData(treeData, 100, 2);
        dataCommunicator.expand(new Item("Item 0"));
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();

        Mockito.verify(dataGenerator)
                .generateData(Mockito.eq(new Item("Item 0")), Mockito.any());
        Mockito.verify(dataGenerator)
                .generateData(Mockito.eq(new Item("Item 0-0")), Mockito.any());
        Mockito.verify(dataGenerator)
                .generateData(Mockito.eq(new Item("Item 0-1")), Mockito.any());
        Mockito.verify(dataGenerator)
                .generateData(Mockito.eq(new Item("Item 1")), Mockito.any());
        Mockito.verify(dataGenerator, Mockito.never())
                .generateData(Mockito.eq(new Item("Item 2")), Mockito.any());

        dataCommunicator.setViewportRange(98, 4);
        fakeClientCommunication();

        Mockito.verify(dataGenerator, Mockito.never())
                .generateData(Mockito.eq(new Item("Item 95")), Mockito.any());
        Mockito.verify(dataGenerator)
                .generateData(Mockito.eq(new Item("Item 96")), Mockito.any());
        Mockito.verify(dataGenerator)
                .generateData(Mockito.eq(new Item("Item 97")), Mockito.any());
        Mockito.verify(dataGenerator)
                .generateData(Mockito.eq(new Item("Item 98")), Mockito.any());
        Mockito.verify(dataGenerator)
                .generateData(Mockito.eq(new Item("Item 99")), Mockito.any());
    }

    @Test
    public void changeViewportRangeBackAndForth_destroyDataCalledForNoLongerVisibleItemsAfterConfirmation() {
        populateTreeData(treeData, 100, 2);
        dataCommunicator.expand(new Item("Item 0"));

        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        dataCommunicator.confirmUpdate(captureArrayUpdateId());
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(Mockito.any());

        Mockito.clearInvocations(dataGenerator, arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(2, 4);
        fakeClientCommunication();
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(Mockito.any()); // not confirmed yet

        dataCommunicator.confirmUpdate(captureArrayUpdateId());
        Mockito.verify(dataGenerator) //
                .destroyData(new Item("Item 0"));
        Mockito.verify(dataGenerator) //
                .destroyData(new Item("Item 0-0"));
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 0-1"));
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 1"));

        Mockito.clearInvocations(dataGenerator, arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(Mockito.any()); // not confirmed yet

        dataCommunicator.confirmUpdate(captureArrayUpdateId());
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 0"));
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 0-0"));
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 0-1"));
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 1"));
        Mockito.verify(dataGenerator) //
                .destroyData(new Item("Item 2"));
        Mockito.verify(dataGenerator) //
                .destroyData(new Item("Item 3"));
    }

    @Test
    public void collapseItems_destroyDataCalledForCollapsedChildrenInKeyMapper() {
        populateTreeData(treeData, 4, 2, 2);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();

        dataCommunicator.collapse(new Item("Item 0"));

        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 0"));
        Mockito.verify(dataGenerator).destroyData(new Item("Item 0-0"));
        Mockito.verify(dataGenerator).destroyData(new Item("Item 0-0-0"));
        Mockito.verify(dataGenerator).destroyData(new Item("Item 0-0-1"));
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 0-1"));
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 1"));
    }

    @Test
    public void collapseItems_destroyDataCalledBeforeItemRemovedFromKeyMapper() {
        populateTreeData(treeData, 4, 2, 2);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();

        Mockito.doAnswer((invocation) -> {
            Item item = invocation.getArgument(0);
            if (!dataCommunicator.getKeyMapper().has(item)) {
                throw new AssertionError(
                        "Item should still be in keyMapper when destroyData is called");
            }
            return null;
        }).when(dataGenerator).destroyData(Mockito.any());

        dataCommunicator.collapse(new Item("Item 0"));
    }
}
