package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.ArrayUpdater;
import com.vaadin.flow.data.provider.ArrayUpdater.Update;
import com.vaadin.flow.data.provider.DataCommunicatorTest;

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

        public void setState(String state) {
            this.state = state;
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

    /**
     * @param property
     *            the property to check
     * @param expected
     *            the expected property values of items
     */
    protected void assertArrayUpdateItems(String property, String... expected) {
        Assert.assertEquals(Arrays.asList(expected),
                captureArrayUpdateItems().values().stream()
                        .map((item) -> item.get(property).asString()).toList());
    }

    /**
     * @param property
     *            the property to check
     * @param expected
     *            a map where the key is the index of an item and the value is
     *            the expected property value
     */
    protected void assertArrayUpdateItems(String property,
            Map<Integer, String> expected) {
        Assert.assertEquals(expected, captureArrayUpdateItems().entrySet()
                .stream().collect(Collectors.toMap(//
                        (entry) -> entry.getKey(),
                        (entry) -> entry.getValue().get(property).asString())));
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
                Mockito.intThat(index -> start <= index && index < end),
                Mockito.anyList());
    }

    protected void assertArrayUpdateSize(int size) {
        Mockito.verify(arrayUpdater).startUpdate(size);
    }

    @SuppressWarnings("unchecked")
    protected Map<Integer, JsonNode> captureArrayUpdateItems() {
        ArgumentCaptor<List<JsonNode>> itemsCaptor = ArgumentCaptor
                .forClass(List.class);
        ArgumentCaptor<Integer> indexCaptor = ArgumentCaptor
                .forClass(Integer.class);
        Mockito.verify(arrayUpdate, Mockito.atLeast(0))
                .set(indexCaptor.capture(), itemsCaptor.capture());

        Map<Integer, JsonNode> result = new LinkedHashMap<>();
        for (int i = 0; i < indexCaptor.getAllValues().size(); i++) {
            var index = indexCaptor.getAllValues().get(i);
            var items = itemsCaptor.getAllValues().get(i);

            for (var j = 0; j < items.size(); j++) {
                result.put(index + j, items.get(j));
            }
        }
        return result;
    }

    protected int captureArrayUpdateId() {
        var argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(arrayUpdate).commit(argumentCaptor.capture());
        return argumentCaptor.getValue();
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
