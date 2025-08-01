package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.vaadin.flow.data.provider.ArrayUpdater;
import com.vaadin.flow.data.provider.ArrayUpdater.Update;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

@RunWith(MockitoJUnitRunner.class)
abstract public class AbstractHierarchicalDataCommunicatorTest {
    public static class Item {
        private String name;

        public Item(String name) {
            this.name = name;
        }

        public String toString() {
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

    @Captor
    protected ArgumentCaptor<List<JsonValue>> arrayUpdateItemsCaptor;

    @Captor
    protected ArgumentCaptor<Integer> arrayUpdateSizeCaptor;

    @Before
    public void init() {
        Mockito.when(arrayUpdater.startUpdate(Mockito.anyInt()))
                .thenAnswer((answer) -> arrayUpdate);
    }

    protected void assertArrayUpdateItems(String property,
            String... expected) {
        Assert.assertEquals(Arrays.asList(expected),
                captureArrayUpdateItems().stream()
                        .map((item) -> ((JsonObject) item).getString(property))
                        .toList());
    }

    protected void assertArrayUpdateRange(int start, int length) {
        int size = captureArrayUpdateSize();
        int end = start + length;

        Mockito.verify(arrayUpdate,
                start > 0 ? Mockito.times(1) : Mockito.never()).clear(0, start);
        Mockito.verify(arrayUpdate,
                end < size ? Mockito.times(1) : Mockito.never())
                .clear(end, size - end);
        Mockito.verify(arrayUpdate, Mockito.times(1)).set(Mockito.eq(start),
                Mockito.anyList());
    }

    protected void assertArrayUpdateSize(int size) {
        Mockito.verify(arrayUpdater, Mockito.times(1)).startUpdate(size);
    }

    protected void assertArrayUpdaterInitialized() {
        Mockito.verify(arrayUpdater, Mockito.times(1)).initialize();
    }

    protected List<JsonValue> captureArrayUpdateItems() {
        Mockito.verify(arrayUpdate).set(Mockito.anyInt(),
                arrayUpdateItemsCaptor.capture());
        return arrayUpdateItemsCaptor.getValue();
    }

    protected int captureArrayUpdateSize() {
        Mockito.verify(arrayUpdater)
                .startUpdate(arrayUpdateSizeCaptor.capture());
        return arrayUpdateSizeCaptor.getValue();
    }

    protected void populateTreeData(TreeData<Item> treeData,
            int... levelSizes) {
        treeData.clear();
        populateTreeData(treeData, null, levelSizes);
    }

    private void populateTreeData(TreeData<Item> treeData, Item parentItem,
            int... levelSizes) {
        for (int i = 0; i < levelSizes[0]; i++) {
            Item item = new Item(
                    parentItem != null ? parentItem + "-" + i : "Item " + i);
            treeData.addItem(parentItem, item);

            if (levelSizes.length > 1) {
                populateTreeData(treeData, item,
                        Arrays.copyOfRange(levelSizes, 1, levelSizes.length));
            }
        }
    }
}
