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

    protected HierarchicalDataCommunicator<Item> dataCommunicator;

    protected TreeData<Item> treeData = new TreeData<>();

    protected TreeDataProvider<Item> treeDataProvider = new TreeDataProvider<>(
            treeData);

    @Before
    public void init() {
        Mockito.when(arrayUpdater.startUpdate(Mockito.anyInt()))
                .thenAnswer((answer) -> arrayUpdate);
    }

    protected void assertArrayUpdateItems(String property,
            List<String> expected) {
        Mockito.verify(arrayUpdate).set(Mockito.anyInt(),
                arrayUpdateItemsCaptor.capture());

        List<JsonValue> items = arrayUpdateItemsCaptor.getValue();
        Assert.assertEquals(expected,
                items.stream()
                        .map((item) -> ((JsonObject) item).getString(property))
                        .toList());
    }

    protected void assertArrayUpdate(int size, int rangeStart, int rangeLength) {
        int rangeEnd = rangeStart + rangeLength;

        Mockito.verify(arrayUpdater, Mockito.times(1)).startUpdate(size);

        Mockito.verify(arrayUpdate, Mockito.times(rangeStart > 0 ? 1 : 0))
                .clear(0, rangeStart);
        Mockito.verify(arrayUpdate, Mockito.times(rangeEnd < size ? 1 : 0))
                .clear(rangeEnd, size - rangeEnd);

        Mockito.verify(arrayUpdate, Mockito.times(1))
                .set(Mockito.eq(rangeStart), Mockito.anyList());
    }

    protected void fixtureTreeData(int... levelSizes) {
        treeData.clear();
        fixtureTreeData(null, levelSizes);
    }

    protected void fixtureTreeData(Item parentItem, int... levelSizes) {
        for (int i = 0; i < levelSizes[0]; i++) {
            Item item = new Item(
                    parentItem != null ? parentItem + "-" + i
                            : "Item " + i);
            treeData.addItem(parentItem, item);

            if (levelSizes.length > 1) {
                fixtureTreeData(item,
                        Arrays.copyOfRange(levelSizes, 1, levelSizes.length));
            }
        }
    }
}
