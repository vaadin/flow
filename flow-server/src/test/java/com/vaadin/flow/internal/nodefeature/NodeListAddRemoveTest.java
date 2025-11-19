/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.change.AbstractListChange;
import com.vaadin.flow.internal.change.ListAddChange;
import com.vaadin.flow.internal.change.ListClearChange;
import com.vaadin.flow.internal.change.ListRemoveChange;
import com.vaadin.flow.internal.change.NodeAttachChange;
import com.vaadin.flow.internal.change.NodeChange;

public class NodeListAddRemoveTest
        extends AbstractNodeFeatureTest<ElementClassList> {
    protected ElementClassList nodeList = createFeature();

    private List<String> resetToRemoveAfterAddCase() {
        nodeList.clear();
        collectChanges(nodeList);
        return addOriginalItems(4);
    }

    @Test
    public void clear_onlyListClearChange() {
        resetToRemoveAfterAddCase();

        nodeList.clear();
        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        verifyCleared(changes);
        Assert.assertEquals(0, nodeList.size());
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
        Assert.assertEquals(2, changes.size());
        Assert.assertTrue(changes.get(0) instanceof ListRemoveChange<?>);
        Assert.assertTrue(changes.get(1) instanceof ListAddChange<?>);

        ListRemoveChange<?> remove = (ListRemoveChange<?>) changes.get(0);
        Assert.assertEquals(items.size() - 2, remove.getIndex());
        Assert.assertEquals(removed, remove.getRemovedItem());

        ListAddChange<?> add = (ListAddChange<?>) changes.get(1);
        Assert.assertEquals(items.size() - 1, add.getIndex());
        Assert.assertEquals(1, add.getNewItems().size());
        Assert.assertEquals(removed, add.getNewItems().get(0));
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
        Assert.assertEquals(0, changes.size());
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
        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof ListAddChange<?>);

        ListAddChange<?> add = (ListAddChange<?>) changes.get(0);
        Assert.assertEquals(nodeList.size() - 2, add.getIndex());
        Assert.assertEquals(2, add.getNewItems().size());
        items.remove(1);
        Assert.assertEquals(items, add.getNewItems());
    }

    @Test
    public void add_addInBetweenAndRemove_changesAreAdjusted() {
        List<String> items = resetToRemoveAfterAddCase();

        String newItem = "foo";
        nodeList.add(newItem);

        nodeList.add("bar");
        nodeList.add("bar1");

        int index = items.size();
        String item = (String) nodeList.get(index);
        nodeList.remove(index);
        // verify that nodelist is adjusted immediately to avoid memory leaks
        Optional<AbstractListChange<Serializable>> optionalChange = nodeList
                .getChangeTracker().stream().filter(change -> {
                    if (change instanceof ListAddChange<?> addChange) {
                        return addChange.getNewItems().contains(item);
                    }
                    return false;
                }).findFirst();
        Assert.assertFalse(optionalChange.isPresent());
        Assert.assertEquals(2, nodeList.getChangeTracker().size());

        List<NodeChange> changes = collectChanges(nodeList);

        // remove is discarded, the fist add is discarded, others are adjusted
        Assert.assertEquals(2, changes.size());
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
        Assert.assertEquals(1, changes.size());
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
        Assert.assertEquals(2, changes.size());

        Assert.assertTrue(changes.get(0) instanceof ListAddChange<?>);
        Assert.assertTrue(changes.get(1) instanceof ListAddChange<?>);

        ListAddChange<?> add = (ListAddChange<?>) changes.get(0);
        Assert.assertEquals(index - 2, add.getIndex());
        Assert.assertEquals(2, add.getNewItems().size());
        items.remove(0);
        Assert.assertEquals(items, add.getNewItems());

        add = (ListAddChange<?>) changes.get(1);
        Assert.assertEquals(index - 1, add.getIndex());
        Assert.assertEquals(1, add.getNewItems().size());
        Assert.assertEquals("bar", add.getNewItems().get(0));
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
        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof ListAddChange<?>);

        verifyAdded(changes, Arrays.asList(newItem), index);
    }

    @Test
    public void removeOperationAfterDelete_addRemove_subsequentOperationsAreNotAffected() {
        List<String> items = resetToRemoveAfterAddCase();

        nodeList.add("foo");
        int index = items.size();
        nodeList.remove(index);

        nodeList.remove(index - 1);
        // As a result: "remove" and "add" before it are discarded and the last
        // "remove" operation is not affected
        List<NodeChange> changes = collectChanges(nodeList);
        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof ListRemoveChange<?>);

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
        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof ListRemoveChange<?>);

        verifyRemoved(changes, Arrays.asList(items.get(index - 1)), index - 1);
    }

    @Test
    public void clearInBetween_addRemove_removeIsAdjustedProperly() {
        resetToRemoveAfterAddCase();

        // First add some item
        nodeList.add("foo");

        // Now clear everything
        nodeList.clear();

        // Now add and remove an item : after clear any add operation should not
        // matter anymore
        nodeList.add("bar");
        nodeList.remove(0);

        List<NodeChange> changes = collectChanges(nodeList);
        // only one clear changes: add is compensated by remove
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(ListClearChange.class, changes.get(0).getClass());
        // what's important: no any exception causes by incorrect index
        // (IndexOutOfBoundsException)
    }

    @Test
    public void clear_collectChanges_allPreviousEventsAreRemoved() {
        List<String> items = resetToRemoveAfterAddCase();

        int index = items.size();

        nodeList.add("foo");
        nodeList.add("bar");
        nodeList.remove(index);

        nodeList.clear();

        nodeList.add("baz");

        List<NodeChange> changes = collectChanges(nodeList);

        Assert.assertEquals(2, changes.size());
        MatcherAssert.assertThat(changes.get(0),
                CoreMatchers.instanceOf(ListClearChange.class));
        MatcherAssert.assertThat(changes.get(1),
                CoreMatchers.instanceOf(ListAddChange.class));

        Assert.assertEquals(1, nodeList.size());
        Assert.assertEquals("baz", nodeList.get(0));
    }

    @Test
    public void clear_collectChanges_resetChangeTracker_clearEventIsCollected() {
        resetToRemoveAfterAddCase();

        nodeList.add("foo");

        nodeList.clear();

        StateTree tree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        // attach the feature node to the tree
        tree.getRootNode().getFeature(ElementChildrenList.class)
                .add(nodeList.getNode());

        nodeList.add("bar");

        List<NodeChange> changes = new ArrayList<>();
        // this call will remove the clear change which has happened before
        // attach, but it should still be collected
        nodeList.getNode().collectChanges(changes::add);

        Assert.assertEquals(3, changes.size());
        MatcherAssert.assertThat(changes.get(0),
                CoreMatchers.instanceOf(NodeAttachChange.class));
        MatcherAssert.assertThat(changes.get(1),
                CoreMatchers.instanceOf(ListClearChange.class));
        MatcherAssert.assertThat(changes.get(2),
                CoreMatchers.instanceOf(ListAddChange.class));

        nodeList.add("baz");

        changes.clear();
        nodeList.getNode().collectChanges(changes::add);
        // Now there is no anymore clear change (so the previous one is not
        // preserved)
        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof ListAddChange<?>);
    }

    @Test
    public void clear_collectChanges_resetChangeTracker_reattach_clearEventIsCollected() {
        resetToRemoveAfterAddCase();

        nodeList.add("foo");

        nodeList.clear();

        StateTree tree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        // attach the feature node to the tree
        tree.getRootNode().getFeature(ElementChildrenList.class)
                .add(nodeList.getNode());

        nodeList.add("bar");

        // detach the feature node to the tree

        tree.getRootNode().getFeature(ElementChildrenList.class).remove(0);
        // if there was no changes collection after detach (and node is attached
        // again) then the node has not been detached de-facto: detach-attach is
        // no-op in this case, so to avoid no-op the changes should be collected
        // in between
        nodeList.getNode().collectChanges(change -> {
        });

        // reattach it back
        tree.getRootNode().getFeature(ElementChildrenList.class)
                .add(nodeList.getNode());

        List<NodeChange> changes = new ArrayList<>();
        nodeList.getNode().collectChanges(changes::add);

        Assert.assertEquals(3, changes.size());
        Assert.assertEquals(NodeAttachChange.class, changes.get(0).getClass());
        Assert.assertEquals(ListClearChange.class, changes.get(1).getClass());
        Assert.assertEquals(ListAddChange.class, changes.get(2).getClass());

        changes.clear();

        nodeList.add("baz");

        nodeList.getNode().collectChanges(changes::add);
        // Now there is no anymore clear change (so the previous one is not
        // preserved)
        Assert.assertEquals(1, changes.size());
        Assert.assertTrue(changes.get(0) instanceof ListAddChange<?>);
    }

    @Test
    public void clearNodeList_clearChanges_generateChangesFromEmpty_clearChangeIsCollected() {
        // removes all children
        nodeList.clear();

        StateTree tree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        // attach the feature node to the tree
        tree.getRootNode().getFeature(ElementChildrenList.class)
                .add(nodeList.getNode());

        List<NodeChange> changes = new ArrayList<>();
        nodeList.getNode().collectChanges(changes::add);

        Assert.assertEquals(2, changes.size());
        Assert.assertEquals(NodeAttachChange.class, changes.get(0).getClass());
        Assert.assertEquals(ListClearChange.class, changes.get(1).getClass());
    }

    @Test
    public void clearNodeList_clearChanges_reatach_generateChangesFromEmpty_clearChangeIsCollected() {
        // removes all children
        nodeList.clear();

        StateTree tree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        // attach the feature node to the tree
        tree.getRootNode().getFeature(ElementChildrenList.class)
                .add(nodeList.getNode());

        nodeList.getNode().collectChanges(change -> {
        });

        // detach the feature node to the tree

        tree.getRootNode().getFeature(ElementChildrenList.class).remove(0);
        // if there was no changes collection after detach (and node is attached
        // again) then the node has not been detached de-facto: detach-attach is
        // no-op in this case, so to avoid no-op the changes should be collected
        // in between
        nodeList.getNode().collectChanges(change -> {
        });

        // reattach it back
        tree.getRootNode().getFeature(ElementChildrenList.class)
                .add(nodeList.getNode());

        List<NodeChange> changes = new ArrayList<>();
        nodeList.getNode().collectChanges(changes::add);

        Assert.assertEquals(2, changes.size());
        Assert.assertEquals(NodeAttachChange.class, changes.get(0).getClass());
        Assert.assertEquals(ListClearChange.class, changes.get(1).getClass());
    }

    @Test
    public void collectChanges_clearNodeListIsDoneFirst_noClearEventinCollectedChanges() {
        // removes all children
        nodeList.clear();

        StateTree tree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        // attach the feature node to the tree
        tree.getRootNode().getFeature(ElementChildrenList.class)
                .add(nodeList.getNode());

        List<NodeChange> changes = new ArrayList<>();
        nodeList.getNode().collectChanges(changes::add);

        changes.clear();

        nodeList.add("foo");

        nodeList.getNode().collectChanges(changes::add);
        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(ListAddChange.class, changes.get(0).getClass());
    }

    @Test
    public void clear_modifyList_collectChanges_clearChangeIsCollected() {
        nodeList.clear();
        nodeList.add("foo");

        StateTree tree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class);
        // attach the feature node to the tree
        tree.getRootNode().getFeature(ElementChildrenList.class)
                .add(nodeList.getNode());

        List<NodeChange> changes = new ArrayList<>();
        nodeList.getNode().collectChanges(changes::add);

        Assert.assertEquals(3, changes.size());
        Assert.assertEquals(NodeAttachChange.class, changes.get(0).getClass());
        Assert.assertEquals(ListClearChange.class, changes.get(1).getClass());
        Assert.assertEquals(ListAddChange.class, changes.get(2).getClass());
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
            Assert.assertEquals(items[i], nodeList.get(i));
        }
    }

    private void verifyCleared(List<NodeChange> changes) {
        Assert.assertEquals(1, changes.size());
        NodeChange nodeChange = changes.get(0);
        MatcherAssert.assertThat(nodeChange,
                CoreMatchers.instanceOf(ListClearChange.class));
    }

    private void verifyRemoved(List<NodeChange> changes, List<String> items,
            Integer... indexes) {
        Assert.assertTrue(changes.size() > 0);
        for (int i = 0; i < indexes.length; i++) {
            NodeChange nodeChange = changes.get(i);
            MatcherAssert.assertThat(nodeChange,
                    CoreMatchers.instanceOf(ListRemoveChange.class));
            ListRemoveChange<?> change = (ListRemoveChange<?>) nodeChange;
            Assert.assertEquals(indexes[i].intValue(), change.getIndex());
            Assert.assertEquals(items.get(i), change.getRemovedItem());
        }
    }

    private void verifyAdded(List<NodeChange> changes, List<String> items,
            Integer... indexes) {
        for (int i = 0; i < indexes.length; i++) {
            NodeChange nodeChange = changes.get(i);
            MatcherAssert.assertThat(nodeChange,
                    CoreMatchers.instanceOf(ListAddChange.class));
            ListAddChange<?> change = (ListAddChange<?>) nodeChange;
            Assert.assertEquals(indexes[i].intValue(), change.getIndex());
            Assert.assertEquals(1, change.getNewItems().size());
            Assert.assertEquals(items.get(i), change.getNewItems().get(0));
        }
    }

}
