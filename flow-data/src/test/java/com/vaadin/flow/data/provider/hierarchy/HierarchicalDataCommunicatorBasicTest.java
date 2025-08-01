package com.vaadin.flow.data.provider.hierarchy;

import java.io.Serializable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataCommunicatorTest;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;

import elemental.json.JsonArray;

public class HierarchicalDataCommunicatorBasicTest
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
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
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
        TreeData<Item> treeData = new TreeData<>();
        TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(treeData);

        dataCommunicator.setDataProvider(treeDataProvider, null);
        Assert.assertEquals(treeDataProvider, dataCommunicator.getDataProvider());
    }

    @Test
    public void setIncompatibleDataProvider_throws() {
        ListDataProvider<Item> incompatibleDataProvider = DataProvider.ofItems(new Item("Item 0"));

        Assert.assertThrows(IllegalArgumentException.class,
                () -> dataCommunicator.setDataProvider(incompatibleDataProvider, null));
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }
}
