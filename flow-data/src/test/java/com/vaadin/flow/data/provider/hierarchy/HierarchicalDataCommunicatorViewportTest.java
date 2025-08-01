package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataCommunicatorTest;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;

import elemental.json.JsonArray;

public class HierarchicalDataCommunicatorViewportTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private DataCommunicatorTest.MockUI ui = new DataCommunicatorTest.MockUI();

    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    private CompositeDataGenerator<Item> dataGenerator = new CompositeDataGenerator<>();
    private SerializableConsumer<JsonArray> dataUpdater = (items) -> {
    };
    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @Before
    public void init() {
        super.init();

        Element element = new Element("div");

        dataGenerator.addDataGenerator((item, json) -> {
            json.put("name", String.valueOf(item));
        });

        dataCommunicator = new HierarchicalDataCommunicator<>(dataGenerator,
                arrayUpdater, dataUpdater, element.getNode(), () -> null);
        dataCommunicator.setDataProvider(treeDataProvider, null);

        ui.getElement().appendChild(element);
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    public void setViewportRange_flush_requestedRangeSent() {
        fixtureTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 5);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 5);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 1",
                "Item 2", "Item 3", "Item 4"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(95, 5);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(95, 5);
        assertArrayUpdateItems("name", Arrays.asList("Item 95", "Item 96",
                "Item 97", "Item 98", "Item 99"));
    }

    @Test
    public void setViewportRangeMultipleTimes_flush_onlyLastRangeSent() {
        fixtureTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 10);
        dataCommunicator.setViewportRange(50, 2);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(50, 2);
        assertArrayUpdateItems("name", Arrays.asList("Item 50", "Item 51"));
    }

    @Test
    public void setViewportRange_toggleItems_rangeItemsUpdated() {
        fixtureTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 6);

        dataCommunicator.expand(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdateSize(102);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 0-0",
                "Item 0-1", "Item 1", "Item 2", "Item 3"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 0-0"));
        fakeClientCommunication();

        assertArrayUpdateSize(104);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 0-0",
                "Item 0-0-0", "Item 0-0-1", "Item 0-1", "Item 1"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0-0"));
        fakeClientCommunication();

        assertArrayUpdateSize(102);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 0-0",
                "Item 0-1", "Item 1", "Item 2", "Item 3"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 1",
                "Item 2", "Item 3", "Item 4", "Item 5"));
    }

    @Test
    public void setViewportRange_toggleItemWithPreExpandedChildren_rangeItemsUpdated() {
        fixtureTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 6);
        dataCommunicator.expand(new Item("Item 0-0"));
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 1",
                "Item 2", "Item 3", "Item 4", "Item 5"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdateSize(104);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 0-0",
                "Item 0-0-0", "Item 0-0-1", "Item 0-1", "Item 1"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 1",
                "Item 2", "Item 3", "Item 4", "Item 5"));
    }

    @Test
    public void setViewportRange_toggleItemOutsideRange_flatSizeNotUpdated() {
        fixtureTreeData(treeData, 100, 2, 2);
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
        fixtureTreeData(treeData, 100, 2, 2);
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

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }
}
