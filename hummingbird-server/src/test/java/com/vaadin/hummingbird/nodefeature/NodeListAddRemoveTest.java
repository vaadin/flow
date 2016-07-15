package com.vaadin.hummingbird.nodefeature;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.change.ListAddChange;
import com.vaadin.hummingbird.change.ListRemoveChange;
import com.vaadin.hummingbird.change.NodeChange;

public class NodeListAddRemoveTest
        extends AbstractNodeFeatureTest<ElementClassList> {
    protected ElementClassList nodeList = createFeature();

    private List<String> resetToRemoveAfterAddCase() {
        nodeList.clear();
        collectChanges(nodeList);
        return addOriginalItems(4);
    }

    @Test
    public void testClear() {
        resetToRemoveAfterAddCase();

        nodeList.clear();
        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 3, 2, 1, 0);
        Assert.assertEquals(0, nodeList.size());

        // add some new items that should not be added after clear
        List<String> items = resetToRemoveAfterAddCase();
        addItems(items, 1, 2, 4);

        nodeList.clear();
        changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 3, 2, 1, 0);
        Assert.assertEquals(0, nodeList.size());
    }

    @Test
    public void testRemoveAfterAdd_addBeginningToSameIndex_removeFromBeginning() {
        List<String> items = resetToRemoveAfterAddCase();

        addItems(items, 0, 0, 4);
        // remove couple
        removeItems(items, 0, 0, 3);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyAdd(changes, 0, 0, "a-0:0/4");

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 0, 4);
        // remove most
        removeItems(items, 0, 0, 6);

        changes = collectChanges(nodeList);

        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 0, 0);

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 0, 4);
        // remove all
        removeItems(items, 0, 0, 8);

        changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 0, 0, 0, 0);

        Assert.assertEquals(0, nodeList.size());
    }

    @Test
    public void testRemoveAfterAdd_addBeginningToSameIndex_removeFromEnd() {
        List<String> items = resetToRemoveAfterAddCase();

        addItems(items, 0, 0, 4);
        // remove all
        removeItems(items, 7, -1, 8);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 3, 2, 1, 0);

        Assert.assertEquals(0, nodeList.size());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 0, 4);
        // remove couple
        removeItems(items, 7, -1, 2);

        changes = collectChanges(nodeList);

        Assert.assertEquals(5, changes.size());
        verifyRemoved(changes, 3, 2);
        verifyAdd(changes, 1, 0, "a-0:0/4");
        verifyAdd(changes, 2, 0, "a-0:1/4");
        verifyAdd(changes, 3, 0, "a-0:2/4");
        verifyAdd(changes, 4, 0, "a-0:3/4");

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 0, 4);
        // remove most
        removeItems(items, 7, -1, 6);

        changes = collectChanges(nodeList);
        Assert.assertEquals(3, changes.size());
        verifyRemoved(changes, 3, 2, 1, 0);
        verifyAdd(changes, 1, 0, "a-0:2/4");
        verifyAdd(changes, 2, 0, "a-0:3/4");

        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addBeginningToSameIndex_removeFromMiddle() {
        List<String> items = resetToRemoveAfterAddCase();

        addItems(items, 0, 0, 4);
        removeItems(items, 2, 0, 4);

        List<NodeChange> changes = collectChanges(nodeList);

        Assert.assertEquals(3, changes.size());
        verifyRemoved(changes, 0, 0);
        verifyAdd(changes, 1, 0, "a-0:2/4");
        verifyAdd(changes, 2, 0, "a-0:3/4");

        Assert.assertEquals(4, nodeList.size());
        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addBeginningToNextIndex_removeFromBeginning() {
        List<String> items = resetToRemoveAfterAddCase();

        addItems(items, 0, 1, 4);
        // remove all
        removeItems(items, 0, 0, 8);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 0, 0, 0, 0);

        Assert.assertEquals(0, nodeList.size());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 1, 4);
        // remove just added
        removeItems(items, 0, 0, 4);

        changes = collectChanges(nodeList);
        Assert.assertEquals(0, changes.size());

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 1, 4);
        // remove couple
        removeItems(items, 0, 0, 2);

        changes = collectChanges(nodeList);
        Assert.assertEquals(2, changes.size());
        verifyAdd(changes, 0, 0, "a-2:2/4");
        verifyAdd(changes, 1, 1, "a-3:3/4");

        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addBeginningToNextIndex_removeFromEnd() {
        List<String> items = resetToRemoveAfterAddCase();

        addItems(items, 0, 1, 4);
        // remove all
        removeItems(items, 7, -1, 8);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 3, 2, 1, 0);

        Assert.assertEquals(0, nodeList.size());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 1, 4);
        // remove couple
        removeItems(items, 7, -1, 2);

        changes = collectChanges(nodeList);
        Assert.assertEquals(5, changes.size());
        verifyRemoved(changes, 3, 2);
        verifyAdd(changes, 1, 0, "a-0:0/4");
        verifyAdd(changes, 2, 1, "a-1:1/4");
        verifyAdd(changes, 3, 2, "a-2:2/4");
        verifyAdd(changes, 4, 3, "a-3:3/4");

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 1, 4);
        // remove most
        removeItems(items, 7, -1, 6);

        changes = collectChanges(nodeList);
        Assert.assertEquals(3, changes.size());
        verifyRemoved(changes, 3, 2, 1, 0);
        verifyAdd(changes, 1, 0, "a-0:0/4");
        verifyAdd(changes, 2, 1, "a-1:1/4");

        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addBeginningToNextIndex_removeFromMiddle() {
        List<String> items = resetToRemoveAfterAddCase();

        addItems(items, 0, 1, 4);

        removeItems(items, 2, 0, 4);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(3, changes.size());
        verifyRemoved(changes, 0, 0);
        verifyAdd(changes, 1, 0, "a-0:0/4");
        verifyAdd(changes, 2, 1, "a-1:1/4");

        Assert.assertEquals(4, nodeList.size());
        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addWithInterval_removeFromBeginning() {
        List<String> items = resetToRemoveAfterAddCase();

        addItems(items, 0, 2, 4);
        // remove all
        removeItems(items, 0, 0, 8);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 0, 0, 0, 0);

        Assert.assertEquals(0, nodeList.size());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 2, 4);
        // remove couple
        removeItems(items, 0, 0, 2);

        changes = collectChanges(nodeList);
        Assert.assertEquals(4, changes.size());
        verifyRemoved(changes, 0);
        verifyAdd(changes, 1, 0, "a-2:1/4");
        verifyAdd(changes, 2, 2, "a-4:2/4");
        verifyAdd(changes, 3, 4, "a-6:3/4");

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 2, 4);
        // remove most
        removeItems(items, 0, 0, 6);

        changes = collectChanges(nodeList);
        Assert.assertEquals(2, changes.size());
        verifyRemoved(changes, 0, 0, 0);
        verifyAdd(changes, 1, 0, "a-6:3/4");

        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addWithInterval_removeFromEnd() {
        List<String> items = resetToRemoveAfterAddCase();

        addItems(items, 0, 2, 4);
        // remove all
        removeItems(items, 7, -1, 8);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 3, 2, 1, 0);

        Assert.assertEquals(0, nodeList.size());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 2, 4);
        // remove couple
        removeItems(items, 7, -1, 3);

        changes = collectChanges(nodeList);
        Assert.assertEquals(4, changes.size());
        verifyRemoved(changes, 3, 2);
        verifyAdd(changes, 1, 0, "a-0:0/4");
        verifyAdd(changes, 2, 2, "a-2:1/4");
        verifyAdd(changes, 3, 4, "a-4:2/4");

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItems(items, 0, 2, 4);
        // remove most
        removeItems(items, 7, -1, 6);

        changes = collectChanges(nodeList);
        Assert.assertEquals(2, changes.size());
        verifyRemoved(changes, 3, 2, 1);
        verifyAdd(changes, 1, 0, "a-0:0/4");

        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addWithInterval_removeFromMiddle() {
        List<String> items = resetToRemoveAfterAddCase();

        addItems(items, 0, 2, 4);
        // a1 o1 a2 o2 a3 o3 a4 o4
        removeItems(items, 2, 0, 4);
        // a1 o1 a4 o4
        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(3, changes.size());
        verifyRemoved(changes, 1, 1);
        verifyAdd(changes, 1, 0, "a-0:0/4");
        verifyAdd(changes, 2, 2, "a-6:3/4");

        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addAllToEnd_removeFromBeginning() {
        List<String> items = resetToRemoveAfterAddCase();

        addItemsAll(items, 2);
        addItemsAll(items, 2);
        // remove all
        removeItems(items, 0, 0, 8);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 0, 0, 0, 0);

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItemsAll(items, 2);
        addItemsAll(items, 2);
        // remove couple
        removeItems(items, 0, 0, 3);

        changes = collectChanges(nodeList);
        Assert.assertEquals(3, changes.size());
        verifyRemoved(changes, 0, 0, 0);
        verifyAdd(changes, 1, 1, "a-4:0/2", "a-5:1/2");
        verifyAdd(changes, 2, 3, "a-6:0/2", "a-7:1/2");

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItemsAll(items, 2);
        addItemsAll(items, 2);
        // remove most
        removeItems(items, 0, 0, 5);

        changes = collectChanges(nodeList);
        Assert.assertEquals(3, changes.size());
        verifyRemoved(changes, 0, 0, 0, 0);
        verifyAdd(changes, 1, 0, "a-5:1/2");
        verifyAdd(changes, 2, 1, "a-6:0/2", "a-7:1/2");

        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addAllToEnd_removeFromMiddle() {
        List<String> items = resetToRemoveAfterAddCase();

        addItemsAll(items, 2);
        addItemsAll(items, 2);
        // remove one original and first addAll
        removeItems(items, 3, 0, 3);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(2, changes.size());
        verifyRemoved(changes, 3);
        verifyAdd(changes, 1, 3, "a-6:0/2", "a-7:1/2");

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItemsAll(items, 2);
        addItemsAll(items, 2);
        // remove with interval
        removeItems(items, 0, 1, 4);

        changes = collectChanges(nodeList);
        Assert.assertEquals(3, changes.size());
        verifyRemoved(changes, 0, 1);
        verifyAdd(changes, 1, 2, "a-5:1/2");
        verifyAdd(changes, 2, 3, "a-7:1/2");

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItemsAll(items, 2);
        addItemsAll(items, 2);
        // remove backwards interval
        removeItems(items, 6, -2, 4);

        changes = collectChanges(nodeList);
        Assert.assertEquals(3, changes.size());
        verifyRemoved(changes, 2, 0);
        verifyAdd(changes, 1, 2, "a-5:1/2");
        verifyAdd(changes, 2, 3, "a-7:1/2");

        verifyNodeListContent(items.toArray());
    }

    @Test
    public void testRemoveAfterAdd_addAllToEnd_removeFromEnd() {
        List<String> items = resetToRemoveAfterAddCase();

        addItemsAll(items, 2);
        addItemsAll(items, 2);
        // remove all
        removeItems(items, 7, -1, 8);

        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 3, 2, 1, 0);

        Assert.assertEquals(0, nodeList.size());

        items = resetToRemoveAfterAddCase();

        addItemsAll(items, 2);
        addItemsAll(items, 2);
        // remove couple
        removeItems(items, 7, -1, 3);

        changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyAdd(changes, 0, 4, "a-4:0/2");

        verifyNodeListContent(items.toArray());

        items = resetToRemoveAfterAddCase();

        addItemsAll(items, 2);
        addItemsAll(items, 2);
        // remove most
        removeItems(items, 7, -1, 5);

        changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyRemoved(changes, 3);

        verifyNodeListContent(items.toArray());
    }

    private List<String> addOriginalItems(int numberOfOriginalItems) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < numberOfOriginalItems; i++) {
            String item = "o-" + i;
            nodeList.add(item);
            items.add(item);
        }
        collectChanges(nodeList);
        verifyNodeListContent(items.toArray());

        return items;
    }

    protected void addItems(List<String> listToCaptureAdded, int startIndex,
            int delta, int numberOfNewItems) {
        for (int i = startIndex, count = 0; count < numberOfNewItems; count++) {
            String item = "a-" + i + ":" + count + "/" + numberOfNewItems;
            nodeList.add(i, item);
            listToCaptureAdded.add(i, item);
            i += delta;
        }
    }

    protected void addItemsAll(List<String> listCaptureAdded,
            int numberOfItems) {
        List<String> items = new ArrayList<>();
        int indexInList = listCaptureAdded.size();
        for (int i = 0; i < numberOfItems; i++) {
            String item = "a-" + indexInList + ":" + i + "/" + numberOfItems;
            items.add(item);
            indexInList++;
        }
        nodeList.addAll(items);
        listCaptureAdded.addAll(items);
    }

    private void removeItems(List<String> listToCaptureRemoved, int startIndex,
            int delta, int numberOfRemoved) {
        for (int i = startIndex, count = 0; count < numberOfRemoved; count++) {
            nodeList.remove(i);
            listToCaptureRemoved.remove(i);
            i += delta;
        }
    }

    private void verifyNodeListContent(Object... items) {
        for (int i = 0; i < items.length; i++) {
            Assert.assertEquals(items[i], nodeList.get(i));
        }
    }

    private void verifyRemoved(List<NodeChange> changes, Integer... indexes) {
        Assert.assertTrue(changes.size() > 0);
        NodeChange firstChange = changes.get(0);
        Assert.assertTrue(firstChange instanceof ListRemoveChange);
        ListRemoveChange removeChange = (ListRemoveChange) firstChange;
        List<Integer> removedIndexes = removeChange.getRemovedIndices();
        Assert.assertEquals(indexes.length, removedIndexes.size());
        Assert.assertArrayEquals(indexes, removedIndexes.toArray());
    }

    private void verifyAdd(List<NodeChange> changes, int indexOfAdd, int index,
            String... values) {
        Assert.assertTrue(changes.size() > indexOfAdd);
        ListAddChange<?> add = (ListAddChange<?>) changes.get(indexOfAdd);
        Assert.assertEquals(index, add.getIndex());
        Assert.assertArrayEquals(values, add.getNewItems().toArray());
    }
}
