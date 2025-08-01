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

public class HierarchicalDataCommunicatorTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private DataCommunicatorTest.MockUI ui = new DataCommunicatorTest.MockUI();
    private CompositeDataGenerator<Item> dataGenerator = new CompositeDataGenerator<>();
    private SerializableConsumer<JsonArray> dataUpdater = (items) -> {
    };
    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @Before
    public void init() {
        super.init();

        Element element = new Element("div");

        dataCommunicator = new HierarchicalDataCommunicator<>(dataGenerator,
                arrayUpdater, dataUpdater, element.getNode(), () -> null);

        ui.getElement().appendChild(element);

        dataGenerator.addDataGenerator((item, json) -> {
            json.put("name", String.valueOf(item));
        });
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    public void emptyDataProvider_flush_emptyRangeSent() {
        fakeClientCommunication();

        Mockito.verify(arrayUpdater, Mockito.times(1)).initialize();
        assertArrayUpdate(0, 0, 0);
    }

    @Test
    public void emptyDataProvider_setViewportRange_flush_emptyRangeSent() {
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Mockito.verify(arrayUpdater, Mockito.times(1)).initialize();
        assertArrayUpdate(0, 0, 0);
    }

    @Test
    public void setDataProvider_flush_emptyRangeSent() {
        fixtureTreeData(100, 2, 2);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        fakeClientCommunication();

        Mockito.verify(arrayUpdater, Mockito.times(1)).initialize();
        assertArrayUpdate(100, 0, 0);
    }

    @Test
    public void setDataProvider_setViewportRange_flush_requestedRangeSent() {
        fixtureTreeData(100, 2, 2);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.setViewportRange(0, 5);
        fakeClientCommunication();

        Mockito.verify(arrayUpdater, Mockito.times(1)).initialize();
        assertArrayUpdate(100, 0, 5);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 1",
                "Item 2", "Item 3", "Item 4"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(95, 5);
        fakeClientCommunication();

        Mockito.verify(arrayUpdater, Mockito.never()).initialize();
        assertArrayUpdate(100, 95, 5);
        assertArrayUpdateItems("name", Arrays.asList("Item 95", "Item 96",
                "Item 97", "Item 98", "Item 99"));
    }

    @Test
    public void setDataProvider_setViewportRangeMultipleTimes_flush_onlyLastRangeSent() {
        fixtureTreeData(100, 2, 2);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.setViewportRange(0, 10);
        dataCommunicator.setViewportRange(50, 2);
        fakeClientCommunication();

        assertArrayUpdate(100, 50, 2);
        assertArrayUpdateItems("name", Arrays.asList("Item 50", "Item 51"));
    }

    @Test
    public void setDataProvider_setViewportRange_toggleItems_updatedRangeSent() {
        fixtureTreeData(100, 2, 2);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.setViewportRange(0, 6);

        dataCommunicator.expand(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdate(102, 0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 0-0",
                "Item 0-1", "Item 1", "Item 2", "Item 3"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 0-0"));
        fakeClientCommunication();

        assertArrayUpdate(104, 0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 0-0",
                "Item 0-0-0", "Item 0-0-1", "Item 0-1", "Item 1"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0-0"));
        fakeClientCommunication();

        assertArrayUpdate(102, 0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 0-0",
                "Item 0-1", "Item 1", "Item 2", "Item 3"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdate(100, 0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 1",
                "Item 2", "Item 3", "Item 4", "Item 5"));
    }

    @Test
    public void setDataProvider_setViewportRange_toggleItemWithPreExpandedChildren_updatedRangeSent() {
        fixtureTreeData(100, 2, 2);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.setViewportRange(0, 6);
        dataCommunicator.expand(new Item("Item 0-0"));
        fakeClientCommunication();

        assertArrayUpdate(100, 0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 1",
                "Item 2", "Item 3", "Item 4", "Item 5"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdate(104, 0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 0-0",
                "Item 0-0-0", "Item 0-0-1", "Item 0-1", "Item 1"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0"));
        fakeClientCommunication();

        assertArrayUpdate(100, 0, 6);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 1",
                "Item 2", "Item 3", "Item 4", "Item 5"));
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }
}
