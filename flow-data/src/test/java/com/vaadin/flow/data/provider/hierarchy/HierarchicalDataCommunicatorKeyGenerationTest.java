package com.vaadin.flow.data.provider.hierarchy;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataCommunicatorTest;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;

import elemental.json.JsonArray;

public class HierarchicalDataCommunicatorKeyGenerationTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private DataCommunicatorTest.MockUI ui = new DataCommunicatorTest.MockUI();

    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    private ValueProvider<Item, String> uniqueKeyProvider = null;
    private CompositeDataGenerator<Item> dataGenerator = new CompositeDataGenerator<>();
    private SerializableConsumer<JsonArray> dataUpdater = (items) -> {
    };
    private HierarchicalDataCommunicator<Item> dataCommunicator;
    private DataKeyMapper<Item> keyMapper;

    @Before
    public void init() {
        super.init();

        Element element = new Element("div");

        dataCommunicator = new HierarchicalDataCommunicator<>(dataGenerator,
                arrayUpdater, dataUpdater, element.getNode(),
                () -> uniqueKeyProvider);
        dataCommunicator.setDataProvider(treeDataProvider, null);

        keyMapper = dataCommunicator.getKeyMapper();

        ui.getElement().appendChild(element);
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    public void changeViewportRangeBackAndForth_generatedKeysMatchItems() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.expand(new Item("Item 0"));

        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 0")),
                keyMapper.key(new Item("Item 0-0")),
                keyMapper.key(new Item("Item 0-1")),
                keyMapper.key(new Item("Item 1")));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(98, 4);
        fakeClientCommunication();
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 96")),
                keyMapper.key(new Item("Item 97")),
                keyMapper.key(new Item("Item 98")),
                keyMapper.key(new Item("Item 99")));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 0")),
                keyMapper.key(new Item("Item 0-0")),
                keyMapper.key(new Item("Item 0-1")),
                keyMapper.key(new Item("Item 1")),
                keyMapper.key(new Item("Item 2")),
                keyMapper.key(new Item("Item 3")));
    }

    @Test
    public void changeViewportRangeBackAndForth_reset_generatedKeysMatchItems() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.expand(new Item("Item 0"));

        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        dataCommunicator.setViewportRange(98, 4);
        fakeClientCommunication();
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.reset();
        fakeClientCommunication();
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 0")),
                keyMapper.key(new Item("Item 0-0")),
                keyMapper.key(new Item("Item 0-1")),
                keyMapper.key(new Item("Item 1")),
                keyMapper.key(new Item("Item 2")),
                keyMapper.key(new Item("Item 3")));
    }

    @Test
    public void toggleItems_collapsedChildrenRemovedFromKeyMapper() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 0"));
        fakeClientCommunication();
        Assert.assertTrue(keyMapper.has(new Item("Item 0-0")));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 0"));
        fakeClientCommunication();
        Assert.assertFalse(keyMapper.has(new Item("Item 0-0")));
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }
}
