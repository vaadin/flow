package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;

public class HierarchicalDataCommunicatorDataRefreshTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    private CompositeDataGenerator<Item> compositeDataGenerator = new CompositeDataGenerator<>();
    private HierarchicalDataCommunicator<Item> dataCommunicator;
    private DataKeyMapper<Item> keyMapper;

    @Before
    public void init() {
        super.init();

        dataCommunicator = new HierarchicalDataCommunicator<>(
                compositeDataGenerator, arrayUpdater, ui.getElement().getNode(),
                () -> null);
        dataCommunicator.setDataProvider(treeDataProvider, null);

        keyMapper = dataCommunicator.getKeyMapper();

        compositeDataGenerator.addDataGenerator((item, json) -> {
            json.put("name", item.getName());
            json.put("state", item.getState());
        });
    }

    @Test
    public void refreshItemThroughDataProvider_refreshMethodCalled() {
        var dataCommunicatorSpy = Mockito.spy(dataCommunicator);

        // Set data provider again to ensure spied DataCommunicator
        // registers data provider listeners with the spied handlers,
        // as the original listeners hold a reference to the original
        // DataCommunicator methods, which aren't spied.
        dataCommunicatorSpy.setDataProvider(treeDataProvider, null);

        var item = new Item("Item 0");
        treeDataProvider.refreshItem(item);
        Mockito.verify(dataCommunicatorSpy).refresh(item, false);

        treeDataProvider.refreshItem(item, true);
        Mockito.verify(dataCommunicatorSpy).refresh(item, true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void refreshAllThroughDataProvider_resetMethodCalled() {
        var dataCommunicatorSpy = Mockito.spy(dataCommunicator);

        // Set data provider again to ensure spied DataCommunicator
        // registers data provider listeners with the spied handlers,
        // as the original listeners hold a reference to the original
        // DataCommunicator methods, which aren't spied.
        dataCommunicatorSpy.setDataProvider(treeDataProvider, null);

        // setDataProvider() calls reset() internally which is not what
        // we want to test here
        Mockito.clearInvocations(dataCommunicatorSpy);

        treeDataProvider.refreshAll();
        Mockito.verify(dataCommunicatorSpy).reset();
    }

    @Test
    public void refreshRootItems_updatedRangeSent() {
        populateTreeData(treeData, 4);
        dataCommunicator.expand(treeData.getRootItems());
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 2", //
                3, "Item 3"));
        assertArrayUpdateItems("state", Map.of( //
                0, "initial", //
                1, "initial", //
                2, "initial", //
                3, "initial"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        Item item0 = new Item("Item 0", "refreshed");
        dataCommunicator.refresh(item0);

        Item item2 = new Item("Item 2", "refreshed");
        dataCommunicator.refresh(item2);

        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                2, "Item 2" //
        ));
        assertArrayUpdateItems("state", Map.of( //
                0, "refreshed", //
                2, "refreshed"));
    }

    @Test
    public void refreshNestedItems_updatedRangeSent() {
        populateTreeData(treeData, 2, 1, 1);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 0-0", //
                2, "Item 0-0-0", //
                3, "Item 1"));
        assertArrayUpdateItems("state", Map.of( //
                0, "initial", //
                1, "initial", //
                2, "initial", //
                3, "initial"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        Item item0_0 = new Item("Item 0-0", "refreshed");
        dataCommunicator.refresh(item0_0);

        Item item0_0_0 = new Item("Item 0-0-0", "refreshed");
        dataCommunicator.refresh(item0_0_0);

        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                1, "Item 0-0", //
                2, "Item 0-0-0"));
        assertArrayUpdateItems("state", Map.of( //
                1, "refreshed", //
                2, "refreshed"));
    }

    @Test
    public void refreshItemsOutOfViewport_updatedRangeSentOnlyAfterItemEntersViewport() {
        populateTreeData(treeData, 50, 1);
        dataCommunicator.expand(new Item("Item 49"));
        dataCommunicator.setViewportRange(0, 3);
        fakeClientCommunication();
        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        var item = treeData.getRootItems().get(49);
        item.setState("refreshed");
        dataCommunicator.refresh(item);

        fakeClientCommunication();
        Mockito.verifyNoInteractions(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(48, 3);
        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                48, "Item 48", //
                49, "Item 49", //
                50, "Item 49-0"));
        assertArrayUpdateItems("state", Map.of( //
                48, "initial", //
                49, "refreshed", //
                50, "initial"));
    }

    @Test
    public void refreshItems_dataGeneratorRefreshItemCalled() {
        populateTreeData(treeData, 2, 1, 1);

        var dataGenerator = Mockito.spy(new DataGenerator<Item>() {
            @Override
            public void generateData(Item item, ObjectNode json) {
                // NO-OP
            }
        });
        compositeDataGenerator.addDataGenerator(dataGenerator);

        Item item = new Item("Item 0", "refreshed");
        dataCommunicator.refresh(item);
        Mockito.verify(dataGenerator, Mockito.only()).refreshData(item);
    }

    @Test
    public void refreshItems_itemRefreshedInKeyMapper() {
        populateTreeData(treeData, 2, 1, 1);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();

        var keys = captureArrayUpdateItems().values().stream()
                .map((item) -> item.get("key").asText()).toList();
        Assert.assertEquals("initial", keyMapper.get(keys.get(0)).getState());
        Assert.assertEquals("initial", keyMapper.get(keys.get(1)).getState());
        Assert.assertEquals("initial", keyMapper.get(keys.get(2)).getState());
        Assert.assertEquals("initial", keyMapper.get(keys.get(3)).getState());

        Item item0_0 = new Item("Item 0-0", "refreshed");
        dataCommunicator.refresh(item0_0);

        Item item0_0_0 = new Item("Item 0-0-0", "refreshed");
        dataCommunicator.refresh(item0_0_0);

        Assert.assertEquals("initial", keyMapper.get(keys.get(0)).getState());
        Assert.assertEquals("refreshed", keyMapper.get(keys.get(1)).getState());
        Assert.assertEquals("refreshed", keyMapper.get(keys.get(2)).getState());
        Assert.assertEquals("initial", keyMapper.get(keys.get(3)).getState());
    }

    @Test
    public void refreshItemsWithChildren_updatedRangeSent() {
        populateTreeData(treeData, 2, 1, 1);
        dataCommunicator
                .expand(Arrays.asList(new Item("Item 0"), new Item("Item 0-0"),
                        new Item("Item 1"), new Item("Item 1-0")));
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 0-0", //
                2, "Item 0-0-0", //
                3, "Item 1", //
                4, "Item 1-0", //
                5, "Item 1-0-0"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        treeData.setParent(new Item("Item 0-0"), new Item("Item 1"));
        dataCommunicator.refresh(new Item("Item 0"), true);
        dataCommunicator.refresh(new Item("Item 1"), true);

        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 1-0", //
                3, "Item 1-0-0", //
                4, "Item 0-0", //
                5, "Item 0-0-0"));
    }

    @Test
    public void refreshItemsWithChildren_dataGeneratorDestroyItemCalledForChildren() {
        populateTreeData(treeData, 1, 1, 1);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 3);
        fakeClientCommunication();

        var dataGenerator = Mockito.spy(new DataGenerator<Item>() {
            @Override
            public void generateData(Item item, ObjectNode json) {
                // NO-OP
            }
        });
        compositeDataGenerator.addDataGenerator(dataGenerator);

        dataCommunicator.refresh(new Item("Item 0"), true);

        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(new Item("Item 0"));
        Mockito.verify(dataGenerator).destroyData(new Item("Item 0-0"));
        Mockito.verify(dataGenerator).destroyData(new Item("Item 0-0-0"));
    }

    @Test
    public void refreshItemsWithChildren_childrenRemovedFromKeyMapper() {
        populateTreeData(treeData, 1, 1, 1);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 3);
        fakeClientCommunication();

        dataCommunicator.refresh(new Item("Item 0"), true);

        Assert.assertTrue(keyMapper.has(new Item("Item 0")));
        Assert.assertFalse(keyMapper.has(new Item("Item 0-0")));
        Assert.assertFalse(keyMapper.has(new Item("Item 0-0-0")));
    }

    @Test
    public void refreshAllItems_updatedRangeSent() {
        populateTreeData(treeData, 6, 1, 1);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 1"), new Item("Item 1-0")));
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 1-0", //
                3, "Item 1-0-0", //
                4, "Item 2", //
                5, "Item 3"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        treeData.removeItem(new Item("Item 0"));
        treeData.removeItem(new Item("Item 1-0"));
        dataCommunicator.reset();
        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 1", //
                1, "Item 2", //
                2, "Item 3", //
                3, "Item 4", //
                4, "Item 5"));
    }

    @Test
    public void refreshAllItems_dataGeneratorDestroyAllDataCalled() {
        populateTreeData(treeData, 6, 1, 1);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 1"), new Item("Item 1-0")));
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();

        var dataGenerator = Mockito.spy(new DataGenerator<Item>() {
            @Override
            public void generateData(Item item, ObjectNode json) {
                // NO-OP
            }
        });
        compositeDataGenerator.addDataGenerator(dataGenerator);

        dataCommunicator.reset();
        Mockito.verify(dataGenerator).destroyAllData();
        Mockito.verify(dataGenerator, Mockito.never())
                .destroyData(Mockito.any());
    }

    @Test
    public void refreshAllItems_allItemsRemovedFromKeyMapper() {
        populateTreeData(treeData, 1, 1, 1);
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));
        dataCommunicator.setViewportRange(0, 3);
        fakeClientCommunication();

        dataCommunicator.reset();
        Assert.assertFalse(keyMapper.has(new Item("Item 0")));
        Assert.assertFalse(keyMapper.has(new Item("Item 0-0")));
        Assert.assertFalse(keyMapper.has(new Item("Item 0-0-0")));
    }
}
