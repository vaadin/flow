package com.vaadin.flow.data.provider.hierarchy;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataCommunicator;

public class HierarchicalDataCommunicatorViewportTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @Before
    public void init() {
        super.init();

        var compositeDataGenerator = new CompositeDataGenerator<Item>();
        compositeDataGenerator.addDataGenerator((item, json) -> {
            json.put("name", item.getName());
        });

        dataCommunicator = new HierarchicalDataCommunicator<>(
                compositeDataGenerator, arrayUpdater, ui.getElement().getNode(),
                () -> null);
        dataCommunicator.setDataProvider(treeDataProvider, null);
    }

    @Test
    public void setViewportRange_flush_requestedRangeSent() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 5);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 5);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 2", //
                3, "Item 3", //
                4, "Item 4"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(95, 5);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(95, 5);
        assertArrayUpdateItems("name", Map.of( //
                95, "Item 95", //
                96, "Item 96", //
                97, "Item 97", //
                98, "Item 98", //
                99, "Item 99"));
    }

    @Test
    public void setViewportRange_flush_setOverlappingRange_partialRangeSent() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 5);
        fakeClientCommunication();

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(3, 5);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(3, 5);
        assertArrayUpdateItems("name", Map.of( //
                5, "Item 5", //
                6, "Item 6", //
                7, "Item 7"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(0, 10);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 10);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 2", //
                8, "Item 8", //
                9, "Item 9"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(3, 5);
        fakeClientCommunication();

        Mockito.verifyNoInteractions(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(0, 5);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 5);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 2"));
    }

    @Test
    public void setViewportRangeMultipleTimes_flush_onlyLastRangeSent() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 10);
        dataCommunicator.setViewportRange(50, 2);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(50, 2);
        assertArrayUpdateItems("name", Map.of( //
                50, "Item 50", //
                51, "Item 51"));
    }

    @Test
    public void setViewportRange_toggleItems_rangeItemsUpdated() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 6);

        dataCommunicator.expand(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdateSize(102);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 0-0", //
                2, "Item 0-1", //
                3, "Item 1", //
                4, "Item 2", //
                5, "Item 3"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 0-0"));
        fakeClientCommunication();

        assertArrayUpdateSize(104);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 0-0", //
                2, "Item 0-0-0", //
                3, "Item 0-0-1", //
                4, "Item 0-1", //
                5, "Item 1"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0-0"));
        fakeClientCommunication();

        assertArrayUpdateSize(102);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 0-0", //
                2, "Item 0-1", //
                3, "Item 1", //
                4, "Item 2", //
                5, "Item 3"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 2", //
                3, "Item 3", //
                4, "Item 4", //
                5, "Item 5"));
    }

    @Test
    public void setViewportRange_toggleItemWithPreExpandedChildren_rangeItemsUpdated() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 6);
        dataCommunicator.expand(new Item("Item 0-0"));
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 2", //
                3, "Item 3", //
                4, "Item 4", //
                5, "Item 5"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdateSize(104);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 0-0", //
                2, "Item 0-0-0", //
                3, "Item 0-0-1", //
                4, "Item 0-1", //
                5, "Item 1"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 2", //
                3, "Item 3", //
                4, "Item 4", //
                5, "Item 5"));
    }

    @Test
    public void setViewportRange_toggleItemOutsideRange_flatSizeNotUpdated() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 10);
        fakeClientCommunication();

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 99"));
        fakeClientCommunication();
        assertArrayUpdateSize(100);

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 99"));
        fakeClientCommunication();
        assertArrayUpdateSize(100);
    }

    @Test
    public void setViewportRange_expandItemOutsideRange_adjustRangeToIncludeItem_flatSizeUpdated() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 99"));
        fakeClientCommunication();
        assertArrayUpdateSize(100);

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(94, 6);
        fakeClientCommunication();
        assertArrayUpdateSize(102);
    }

    @Test
    public void setViewportRangeWithStartBeyondFlatSize_rangeReset() {
        populateTreeData(treeData, 100, 10);
        dataCommunicator.expand(new Item("Item 99"));

        dataCommunicator.setViewportRange(200, 10);
        fakeClientCommunication();
        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 10);
    }

    @Test
    public void setViewportRangeWithEndBeyondFlatSize_rangeAdjusted() {
        populateTreeData(treeData, 100, 10);
        dataCommunicator.expand(new Item("Item 99"));

        dataCommunicator.setViewportRange(0, 200);
        fakeClientCommunication();
        assertArrayUpdateSize(110);
        assertArrayUpdateRange(0, 110);
    }

    @Test
    public void setTooLargeViewportRange_warningLogged() {
        populateTreeData(treeData, 100, 100);
        dataCommunicator.expand(treeData.getRootItems());

        try (var mockedFactory = Mockito.mockStatic(LoggerFactory.class)) {
            var mockedLogger = Mockito.mock(Logger.class);

            mockedFactory
                    .when(() -> LoggerFactory.getLogger(DataCommunicator.class))
                    .thenReturn(mockedLogger);

            dataCommunicator.setViewportRange(0, 1000);
            Mockito.verify(mockedLogger).warn(Mockito.contains(
                    "number of items requested '1000', maximum items allowed '500'"));

            Mockito.clearInvocations(mockedLogger);

            dataCommunicator.setPageSize(20);
            dataCommunicator.setViewportRange(0, 1000);
            Mockito.verify(mockedLogger).warn(Mockito.contains(
                    "number of items requested '1000', maximum items allowed '500'"));

            Mockito.clearInvocations(mockedLogger);

            dataCommunicator.setPageSize(80);
            dataCommunicator.setViewportRange(0, 1000);
            Mockito.verify(mockedLogger).warn(Mockito.contains(
                    "number of items requested '1000', maximum items allowed '800'"));
        }
    }

    @Test
    public void setTooLargeViewportRange_onlyMaxAllowedRangeSent() {
        populateTreeData(treeData, 100, 100);
        dataCommunicator.expand(treeData.getRootItems());

        dataCommunicator.setViewportRange(0, 1000);
        fakeClientCommunication();
        assertArrayUpdateSize(600);
        assertArrayUpdateRange(0, 500);

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.reset();
        dataCommunicator.setPageSize(20);
        dataCommunicator.setViewportRange(0, 1000);
        fakeClientCommunication();
        assertArrayUpdateSize(600);
        assertArrayUpdateRange(0, 500);

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.reset();
        dataCommunicator.setPageSize(80);
        dataCommunicator.setViewportRange(0, 1000);
        fakeClientCommunication();
        assertArrayUpdateSize(900);
        assertArrayUpdateRange(0, 800);
    }
}
