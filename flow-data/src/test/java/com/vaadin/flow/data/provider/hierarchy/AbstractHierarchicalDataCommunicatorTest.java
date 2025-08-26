package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ArrayUpdater;
import com.vaadin.flow.data.provider.ArrayUpdater.Update;
import com.vaadin.flow.data.provider.DataCommunicatorTest;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

abstract public class AbstractHierarchicalDataCommunicatorTest {
    public static class Item {
        private String name;
        private String state;

        public Item(String name) {
            this.name = name;
            this.state = "initial";
        }

        public Item(String name, String state) {
            this.name = name;
            this.state = state;
        }

        public String getState() {
            return state;
        }

        public String getName() {
            return name;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Item item) {
                return name.equals(item.name);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @Mock
    protected ArrayUpdater arrayUpdater;

    @Mock
    protected Update arrayUpdate;

    protected DataCommunicatorTest.MockUI ui = new DataCommunicatorTest.MockUI();

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        Mockito.when(arrayUpdater.startUpdate(Mockito.anyInt()))
                .thenReturn(arrayUpdate);
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
    }

    protected void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }

    protected void assertArrayUpdateItems(String property, String... expected) {
        Assert.assertEquals(Arrays.asList(expected),
                captureArrayUpdateItems().stream()
                        .map((item) -> ((JsonObject) item).getString(property))
                        .toList());
    }

    protected void assertArrayUpdateRange(int start, int length) {
        var end = start + length;
        var size = captureArrayUpdateSize();

        Mockito.verify(arrayUpdate,
                start > 0 ? Mockito.times(1) : Mockito.never()).clear(0, start);
        Mockito.verify(arrayUpdate,
                end < size ? Mockito.times(1) : Mockito.never())
                .clear(end, size - end);
        Mockito.verify(arrayUpdate, Mockito.atMost(length)).set(
                Mockito.intThat(index -> index >= start && index < end),
                Mockito.anyList());

        var items = captureArrayUpdateItems();
        Assert.assertTrue(items.size() <= Math.min(size, length));
    }

    protected void assertArrayUpdateSize(int size) {
        Mockito.verify(arrayUpdater, Mockito.times(1)).startUpdate(size);
    }

    protected List<JsonValue> captureArrayUpdateItems() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JsonValue>> argumentCaptor = ArgumentCaptor
                .forClass(List.class);

        Mockito.verify(arrayUpdate, Mockito.atLeastOnce()).set(Mockito.anyInt(),
                argumentCaptor.capture());
        return argumentCaptor.getAllValues().stream().flatMap(List::stream)
                .toList();
    }

    protected int captureArrayUpdateSize() {
        var argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(arrayUpdater).startUpdate(argumentCaptor.capture());
        return argumentCaptor.getValue();
    }

    protected void populateTreeData(TreeData<Item> treeData,
            int... levelSizes) {
        treeData.clear();
        populateTreeData(treeData, null, levelSizes);
    }

    private void populateTreeData(TreeData<Item> treeData, Item parentItem,
            int... levelSizes) {
        for (int i = 0; i < levelSizes[0]; i++) {
            Item item = parentItem != null
                    ? new Item(parentItem.getName() + "-" + i)
                    : new Item("Item " + i, "initial");
            treeData.addItem(parentItem, item);

            if (levelSizes.length > 1) {
                populateTreeData(treeData, item,
                        Arrays.copyOfRange(levelSizes, 1, levelSizes.length));
            }
        }
    }
}
