package com.vaadin.flow.data.provider.hierarchy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.function.ValueProvider;

public class HierarchicalDataCommunicatorKeyGenerationTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    private ValueProvider<Item, String> uniqueKeyProvider = null;
    private CompositeDataGenerator<Item> dataGenerator = new CompositeDataGenerator<>();
    private HierarchicalDataCommunicator<Item> dataCommunicator;
    private DataKeyMapper<Item> keyMapper;

    @Before
    public void init() {
        super.init();

        dataCommunicator = new HierarchicalDataCommunicator<>(dataGenerator,
                arrayUpdater, (items) -> {
                }, ui.getElement().getNode(), () -> uniqueKeyProvider);
        dataCommunicator.setDataProvider(treeDataProvider, null);

        keyMapper = dataCommunicator.getKeyMapper();
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
    public void setUniqueKeyProvider_keysGeneratedByProvider() {
        populateTreeData(treeData, 100, 2, 2);
        dataCommunicator.expand(new Item("Item 0"));
        dataCommunicator.setViewportRange(0, 4);

        uniqueKeyProvider = (item) -> {
            return "key-" + item.getName().toLowerCase().replace("item ", "");
        };
        fakeClientCommunication();

        assertArrayUpdateItems("key", "key-0", "key-0-0", "key-0-1", "key-1");
        assertArrayUpdateItems("key", keyMapper.key(new Item("Item 0")),
                keyMapper.key(new Item("Item 0-0")),
                keyMapper.key(new Item("Item 0-1")),
                keyMapper.key(new Item("Item 1")));
    }

    @Test
    public void generateKeyManually_resetBeforeInitFlush_keyPreserved() {
        populateTreeData(treeData, 100, 2, 2);

        var key = keyMapper.key(new Item("Item 4"));
        dataCommunicator.reset();
        Assert.assertEquals(new Item("Item 4"), keyMapper.get(key));
    }
}
