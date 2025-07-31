package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ArrayUpdater;
import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.DataCommunicatorTest;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.data.provider.ArrayUpdater.Update;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

@RunWith(MockitoJUnitRunner.class)
public class HierarchicalDataCommunicatorTest {
    private static class Item {
        private String name;

        public Item(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Mock
    private ArrayUpdater arrayUpdater;

    @Mock
    private Update arrayUpdate;

    @Captor
    private ArgumentCaptor<List<JsonValue>> arrayUpdateItemsCaptor;

    private Element element = new Element("div");
    private DataCommunicatorTest.MockUI ui = new DataCommunicatorTest.MockUI();
    private SerializableConsumer<JsonArray> dataUpdater = (jsonArray) -> {
    };
    private CompositeDataGenerator<Item> dataGenerator = new CompositeDataGenerator<>();
    private HierarchicalDataCommunicator<Item> dataCommunicator;

    private TreeData<Item> treeData = new TreeData<>();
    private TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    @Before
    public void init() {
        dataCommunicator = new HierarchicalDataCommunicator<>(dataGenerator,
                arrayUpdater, dataUpdater, element.getNode(), () -> null);

        ui.getElement().appendChild(element);

        dataGenerator.addDataGenerator((item, json) -> {
            json.put("name", item.getName());
        });

        Mockito.when(arrayUpdater.startUpdate(Mockito.anyInt()))
                .thenAnswer((answer) -> arrayUpdate);
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

        assertArrayUpdate(100, 0, 0);
    }

    @Test
    public void setDataProvider_setViewportRange_flush_requestedRangeSent() {
        fixtureTreeData(100, 2, 2);
        dataCommunicator.setDataProvider(treeDataProvider, null);

        dataCommunicator.setViewportRange(0, 5);
        fakeClientCommunication();

        assertArrayUpdate(100, 0, 5);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 1",
                "Item 2", "Item 3", "Item 4"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(50, 5);
        fakeClientCommunication();

        assertArrayUpdate(100, 50, 55);
        assertArrayUpdateItems("name", Arrays.asList("Item 50", "Item 51",
                "Item 52", "Item 53", "Item 54"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(95, 5);
        fakeClientCommunication();

        assertArrayUpdate(100, 95, 100);
        assertArrayUpdateItems("name", Arrays.asList("Item 95", "Item 96",
                "Item 97", "Item 98", "Item 99"));
    }

    @Test
    public void setDataProvider_setViewportRange_toggleItem_flush_requestedRangeUpdated() {
        fixtureTreeData(100, 2, 2);
        dataCommunicator.setDataProvider(treeDataProvider, null);
        dataCommunicator.setViewportRange(0, 5);

        dataCommunicator.expand(treeData.getRootItems().get(0));
        fakeClientCommunication();

        assertArrayUpdate(102, 0, 5);
        assertArrayUpdateItems("name", Arrays.asList("Item 0", "Item 0-0",
                "Item 0-1", "Item 1", "Item 2"));
    }

    // class WithDataProvider {
    //     @Before
    //     public void init() {
    //         fixtureTreeData(100, 2, 2);
    //         dataCommunicator.setDataProvider(treeDataProvider, null);
    //     }
    // }

    private void fixtureTreeData(int... levelSizes) {
        treeData.clear();
        fixtureTreeData(null, levelSizes);
    }

    private void fixtureTreeData(Item parentItem, int... levelSizes) {
        for (int i = 0; i < levelSizes[0]; i++) {
            Item item = new Item(
                    parentItem != null ? parentItem.getName() + "-" + i
                            : "Item " + i);
            treeData.addItem(parentItem, item);

            if (levelSizes.length > 1) {
                fixtureTreeData(item,
                        Arrays.copyOfRange(levelSizes, 1, levelSizes.length));
            }
        }
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }

    private void assertArrayUpdateItems(String property,
            List<String> expected) {
        Mockito.verify(arrayUpdate).set(Mockito.anyInt(),
                arrayUpdateItemsCaptor.capture());

        List<JsonValue> items = arrayUpdateItemsCaptor.getValue();
        Assert.assertEquals(expected,
                items.stream()
                        .map((item) -> ((JsonObject) item).getString(property))
                        .toList());
    }

    private void assertArrayUpdate(int size, int rangeStart, int rangeEnd) {
        Mockito.verify(arrayUpdater, Mockito.times(1)).startUpdate(size);

        Mockito.verify(arrayUpdate, Mockito.times(rangeStart > 0 ? 1 : 0))
                .clear(0, rangeStart);
        Mockito.verify(arrayUpdate, Mockito.times(rangeEnd < size ? 1 : 0))
                .clear(rangeEnd, size - rangeEnd);

        Mockito.verify(arrayUpdate, Mockito.times(1))
                .set(Mockito.eq(rangeStart), Mockito.anyList());
    }
}
