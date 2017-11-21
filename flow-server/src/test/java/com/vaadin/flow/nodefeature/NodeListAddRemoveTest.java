package com.vaadin.flow.nodefeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.vaadin.flow.change.ListAddChange;
import com.vaadin.flow.change.ListRemoveChange;
import com.vaadin.flow.change.NodeChange;

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
        List<String> items = resetToRemoveAfterAddCase();

        nodeList.clear();
        List<NodeChange> changes = collectChanges(nodeList);
        assertEquals(4, changes.size());
        verifyRemoved(changes, items, 0, 0, 0, 0);
        assertEquals(0, nodeList.size());
    }

    @Test
    public void remove_add_changesAreNotAdjusted() {
        List<String> items = resetToRemoveAfterAddCase();

        // remove an item before the last one
        nodeList.remove(items.size() - 2);

        String removed = items.get(items.size() - 2);
        // add it back to the end
        nodeList.add(removed);

        List<NodeChange> changes = collectChanges(nodeList);

        // normal behavior: one remove and one add change in order
        assertEquals(2, changes.size());
        assertTrue(changes.get(0) instanceof ListRemoveChange<?>);
        assertTrue(changes.get(1) instanceof ListAddChange<?>);

        ListRemoveChange<?> remove = (ListRemoveChange<?>) changes.get(0);
        assertEquals(items.size() - 2, remove.getIndex());
        assertEquals(removed, remove.getRemovedItem());

        ListAddChange<?> add = (ListAddChange<?>) changes.get(1);
        assertEquals(items.size() - 1, add.getIndex());
        assertEquals(1, add.getNewItems().size());
        assertEquals(removed, add.getNewItems().get(0));
    }

    @Test
    public void add_immediatelyRemove_changesDiscarded() {
        List<String> items = resetToRemoveAfterAddCase();

        String newItem = "foo";
        nodeList.add(newItem);
        int index = items.size();
        nodeList.remove(index);

        List<NodeChange> changes = collectChanges(nodeList);

        // changes are discarded
        assertEquals(0, changes.size());
    }

    @Test
    public void addAll_immediatelyRemove_changeIsAdjusted() {
        resetToRemoveAfterAddCase();

        List<String> items = new ArrayList<>();
        // this makes one change with 3 items in it
        addItemsAll(items, 3);

        // remove the second item from recently added
        nodeList.remove(nodeList.size() - 2);

        // As a result: "remove" change is discarded and the "add" is adjusted
        List<NodeChange> changes = collectChanges(nodeList);
        assertEquals(1, changes.size());
        assertTrue(changes.get(0) instanceof ListAddChange<?>);

        ListAddChange<?> add = (ListAddChange<?>) changes.get(0);
        assertEquals(nodeList.size() - 2, add.getIndex());
        assertEquals(2, add.getNewItems().size());
        items.remove(1);
        assertEquals(items, add.getNewItems());
    }

    @Test
    public void add_addInBetweenAndRemove_changesAreAdjusted() {
        List<String> items = resetToRemoveAfterAddCase();

        String newItem = "foo";
        nodeList.add(newItem);

        nodeList.add("bar");
        nodeList.add("bar1");

        int index = items.size();
        nodeList.remove(index);

        List<NodeChange> changes = collectChanges(nodeList);

        // remove is discarded, the fist add is discarded, others are adjusted
        assertEquals(2, changes.size());
        verifyAdded(changes, Arrays.asList("bar", "bar1"), index, index + 1);
    }

    @Test
    public void insertBefore_addAndRemove_changesAreDiscarded() {
        resetToRemoveAfterAddCase();

        nodeList.add("foo");

        int index = nodeList.size() - 1;
        // Now insert an item at the index
        nodeList.add(index, "bar");

        // remove the second item from recently added
        nodeList.remove(nodeList.size() - 1);

        List<NodeChange> changes = collectChanges(nodeList);
        assertEquals(1, changes.size());
        verifyAdded(changes, Arrays.asList("bar"), index);
    }

    @Test
    public void insertAfter_addAllAndRemove_changesAreAdjusted() {
        resetToRemoveAfterAddCase();

        List<String> items = new ArrayList<>();
        // this makes one change with 3 items in it
        addItemsAll(items, 3);

        int index = nodeList.size() - 1;
        // Now insert an item at the index
        nodeList.add(index, "bar");

        // remove the first item from the added above (via "all")
        nodeList.remove(index - 2);

        // As a result: "remove" change is discarded and the "add" are adjusted
        List<NodeChange> changes = collectChanges(nodeList);
        assertEquals(2, changes.size());

        assertTrue(changes.get(0) instanceof ListAddChange<?>);
        assertTrue(changes.get(1) instanceof ListAddChange<?>);

        ListAddChange<?> add = (ListAddChange<?>) changes.get(0);
        assertEquals(index - 2, add.getIndex());
        assertEquals(2, add.getNewItems().size());
        items.remove(0);
        assertEquals(items, add.getNewItems());

        add = (ListAddChange<?>) changes.get(1);
        assertEquals(index - 1, add.getIndex());
        assertEquals(1, add.getNewItems().size());
        assertEquals("bar", add.getNewItems().get(0));
    }

    @Test
    public void addOperationAfterDelete_addRemove_subsequentOoperationsAreNotAffected() {
        List<String> items = resetToRemoveAfterAddCase();

        nodeList.add("foo");
        int index = items.size();
        nodeList.remove(index);

        String newItem = "bar";
        nodeList.add(newItem);

        // As a result: "remove" and "add" before it are discarded and the "add"
        // operation is not affected
        List<NodeChange> changes = collectChanges(nodeList);
        assertEquals(1, changes.size());
        assertTrue(changes.get(0) instanceof ListAddChange<?>);

        verifyAdded(changes, Arrays.asList(newItem), index);
    }

    @Test
    public void removeOperationAfterDelete_addRemove_subsequentOoperationsAreNotAffected() {
        List<String> items = resetToRemoveAfterAddCase();

        nodeList.add("foo");
        int index = items.size();
        nodeList.remove(index);

        nodeList.remove(index - 1);
        // As a result: "remove" and "add" before it are discarded and the last
        // "remove" operation is not affected
        List<NodeChange> changes = collectChanges(nodeList);
        assertEquals(1, changes.size());
        assertTrue(changes.get(0) instanceof ListRemoveChange<?>);

        verifyRemoved(changes, Arrays.asList(items.get(index - 1)), index - 1);
    }

    @Test
    public void removeInBetween_addRemove_removeIsNotAdjusted() {
        List<String> items = resetToRemoveAfterAddCase();

        int index = items.size();
        nodeList.add("foo");

        // remove the item that has been added BEFORE.
        nodeList.remove(index - 1);

        // remove previously added element
        nodeList.remove(index - 1);

        // As a result: "remove" and its corresponding "add" are discarded and
        // the "remove" in between operation is adjusted
        List<NodeChange> changes = collectChanges(nodeList);
        assertEquals(1, changes.size());
        assertTrue(changes.get(0) instanceof ListRemoveChange<?>);

        verifyRemoved(changes, Arrays.asList(items.get(index - 1)), index - 1);
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

    private void addItemsAll(List<String> listCaptureAdded, int numberOfItems) {
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

    private void verifyNodeListContent(Object... items) {
        for (int i = 0; i < items.length; i++) {
            assertEquals(items[i], nodeList.get(i));
        }
    }

    private void verifyRemoved(List<NodeChange> changes, List<String> items,
            Integer... indexes) {
        assertTrue(changes.size() > 0);
        for (int i = 0; i < indexes.length; i++) {
            NodeChange nodeChange = changes.get(i);
            assertTrue(nodeChange instanceof ListRemoveChange);
            ListRemoveChange<?> change = (ListRemoveChange<?>) nodeChange;
            assertEquals(indexes[i].intValue(), change.getIndex());
            assertEquals(items.get(i), change.getRemovedItem());
        }
    }

    private void verifyAdded(List<NodeChange> changes, List<String> items,
            Integer... indexes) {
        for (int i = 0; i < indexes.length; i++) {
            NodeChange nodeChange = changes.get(i);
            assertTrue(nodeChange instanceof ListAddChange);
            ListAddChange<?> change = (ListAddChange<?>) nodeChange;
            assertEquals(indexes[i].intValue(), change.getIndex());
            assertEquals(1, change.getNewItems().size());
            assertEquals(items.get(i), change.getNewItems().get(0));
        }
    }

}
