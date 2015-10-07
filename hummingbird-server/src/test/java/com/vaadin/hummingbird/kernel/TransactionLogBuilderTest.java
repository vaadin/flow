package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeContentsChange;
import com.vaadin.hummingbird.kernel.change.NodeDataChange;
import com.vaadin.hummingbird.kernel.change.NodeListChange;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;
import com.vaadin.server.communication.TransactionLogBuilder;
import com.vaadin.server.communication.TransactionLogOptimizer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TransactionLogBuilderTest {

    private static final String LIST_KEY = "listKey";
    private RootNode root;
    private StateNode node;
    private LinkedHashMap<StateNode, List<NodeChange>> transactionLog;
    private LinkedHashMap<StateNode, List<NodeChange>> optimizedTransactionLog;
    private int transactionLogSize;
    private int optimizedTransactionLogSize;
    private StateNode node1;
    private StateNode node2;
    private StateNode node3;
    private StateNode node4;
    private StateNode node5;

    private static class DebugStateNode extends MapStateNode {
        private String name;

        private DebugStateNode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Before
    public void setup() {
        root = new RootNode();
        node = new DebugStateNode("node");
        root.put(StateNode.class, node);
        node1 = new DebugStateNode("node1");
        node2 = new DebugStateNode("node2");
        node3 = new DebugStateNode("node3");
        node4 = new DebugStateNode("node4");
        node5 = new DebugStateNode("node5");
    }

    @Test
    public void listAddRemovePlain() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add("1");
        list.remove("1");

        commit();

        Assert.assertEquals(
                "Add + remove should generate an add and remove change", 2,
                transactionLogSize);
        assertOptimizedChanges();

    }

    @Test
    public void listAddAddRemovePlain() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add("1");
        list.add("2");
        list.remove("1");

        commit();

        Assert.assertEquals(3, transactionLogSize);
        assertOptimizedChanges(new ListInsertChange(0, LIST_KEY, "2"));

    }

    @Test
    public void listAddAddRemoveSamePlain() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add("1");
        list.add("1");
        list.remove("1");

        commit();

        Assert.assertEquals(3, transactionLogSize);
        assertOptimizedChanges(new ListInsertChange(0, LIST_KEY, "1"));

    }

    @Test
    public void listAddRemove() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.remove(node1);

        commit();

        Assert.assertEquals(
                "Add + remove should generate an add and remove change", 2,
                transactionLogSize);
        assertOptimizedChanges();

    }

    @Test
    public void listAddAddAddRemove() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.add(node2);
        list.add(node3);
        list.remove(node1);

        commit();

        Assert.assertEquals(4, transactionLogSize);

        Assert.assertFalse(
                "No changes related to the removed node should be in the optimized log",
                optimizedLogContainsNode(node1));
        assertOptimizedChanges(new ListInsertChange(0, LIST_KEY, node2),
                new ListInsertChange(1, LIST_KEY, node3));

    }

    private void assertOptimizedChanges(NodeChange... changes) {
        Assert.assertEquals("Unexpected number of changes in the optimized log",
                changes.length, optimizedTransactionLogSize);
        int changeId = 0;
        for (StateNode n : optimizedTransactionLog.keySet()) {
            for (NodeChange change : optimizedTransactionLog.get(n)) {
                assertChangessEquals(changes[changeId++], change);
            }
        }

    }

    private void assertChangessEquals(NodeChange change1, NodeChange change2) {
        if (change1 == null) {
            Assert.assertNull(change2);
        }

        Assert.assertEquals(change1.getClass(), change2.getClass());

        if (change1.getClass() == IdChange.class) {
            assertChangesEquals((IdChange) change1, (IdChange) change2);
        } else if (change1.getClass() == ListInsertChange.class) {
            assertChangesEquals((NodeListChange) change1,
                    (NodeListChange) change2);
        } else if (change1.getClass() == ListRemoveChange.class) {
            assertChangesEquals((NodeListChange) change1,
                    (NodeListChange) change2);
        } else if (change1.getClass() == ListReplaceChange.class) {
            assertChangesEquals((ListReplaceChange) change1,
                    (ListReplaceChange) change2);
        } else if (change1.getClass() == PutChange.class) {
            assertChangesEquals((NodeDataChange) change1,
                    (NodeDataChange) change2);
        } else if (change1.getClass() == RemoveChange.class) {
            assertChangesEquals((NodeDataChange) change1,
                    (NodeDataChange) change2);
        } else if (change1.getClass() == ParentChange.class) {
            assertChangesEquals((ParentChange) change1, (ParentChange) change2);
        } else {
            Assert.fail("Unknown change type: " + change1.getClass().getName());
        }

    }

    private void assertChangesEquals(ParentChange change1,
            ParentChange change2) {
        Assert.assertEquals(change1.getOldParent(), change2.getOldParent());
        Assert.assertEquals(change1.getNewParent(), change2.getNewParent());
    }

    private void assertChangesEquals(ListReplaceChange change1,
            ListReplaceChange change2) {
        assertChangesEquals((NodeListChange) change1, (NodeListChange) change2);
        Assert.assertEquals(change1.getOldValue(), change2.getOldValue());
    }

    private void assertChangesEquals(NodeListChange change1,
            NodeListChange change2) {
        assertChangesEquals((NodeContentsChange) change1,
                (NodeContentsChange) change2);
        Assert.assertEquals(change1.getIndex(), change1.getIndex());
        Assert.assertEquals(change1.getValue(), change1.getValue());
    }

    private void assertChangesEquals(NodeContentsChange change1,
            NodeContentsChange change2) {
        Assert.assertEquals(change1.getKey(), change1.getKey());
    }

    private void assertChangesEquals(NodeDataChange change1,
            NodeDataChange change2) {
        assertChangesEquals((NodeContentsChange) change1,
                (NodeContentsChange) change2);
        Assert.assertEquals(change1.getValue(), change1.getValue());
    }

    private void assertChangesEquals(IdChange change1, IdChange change2) {
        Assert.assertEquals(change1.getOldId(), change2.getOldId());
        Assert.assertEquals(change1.getNewId(), change2.getNewId());
    }

    private boolean optimizedLogContainsNode(StateNode node) {
        return optimizedTransactionLog.containsKey(node);
    }

    @Test
    public void listAddAddRemove() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.add(node2);
        list.remove(node1);

        commit();

        Assert.assertEquals("Add + add + remove should generate 3 changes", 3,
                transactionLogSize);
        Assert.assertFalse(
                "No changes related to the removed node should be in the optimized log",
                optimizedLogContainsNode(node1));
        assertOptimizedChanges(new ListInsertChange(0, LIST_KEY, node2));
    }

    @Test
    public void listInsertWithNodesBeforeAndAfter() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.add(node2);
        // Insert between two existing
        list.add(1, node3);
        // Afterwards add one before and one after the one we are going to
        // remove
        list.add(0, node4);
        list.add(node5);

        // Result is now n4,n1,n3,n2,n5
        list.remove(node3);

        commit();

        Assert.assertEquals(6, transactionLogSize);
        Assert.assertFalse(
                "No changes related to the removed node should be in the optimized log",
                optimizedLogContainsNode(node3));
        assertOptimizedChanges(new ListInsertChange(0, LIST_KEY, node1),
                new ListInsertChange(1, LIST_KEY, node2),
                new ListInsertChange(0, LIST_KEY, node4),
                new ListInsertChange(3, LIST_KEY, node5));
    }

    @Test
    public void listAddUpdateRemove() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        node1.put("foo", "bar");
        list.remove(node1);

        commit();

        Assert.assertEquals(3, transactionLogSize); // add + put + remove
        Assert.assertFalse(
                "No changes related to the removed node should be in the optimized log",
                optimizedLogContainsNode(node1));
        assertOptimizedChanges();
    }

    @Test
    public void listAddRemoveAddSame() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.remove(node1);
        list.add(node1);

        commit();

        Assert.assertEquals(3, transactionLogSize); // add + remove + add
        assertOptimizedChanges(new ListInsertChange(0, LIST_KEY, node1));
    }

    @Test
    public void listAddAddClear() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.add(node2);
        list.clear();
        commit();

        Assert.assertEquals(4, transactionLogSize);
        Assert.assertFalse(
                "No changes related to the removed node should be in the optimized log",
                optimizedLogContainsNode(node1));
        Assert.assertFalse(
                "No changes related to the removed node should be in the optimized log",
                optimizedLogContainsNode(node2));
        assertOptimizedChanges();
    }

    @Test
    public void detachedNodesRemoved() {
    }

    private List<NodeChange> flatten(
            LinkedHashMap<StateNode, List<NodeChange>> transactionLog) {
        List<NodeChange> list = new ArrayList<>();
        for (StateNode n : transactionLog.keySet()) {
            for (NodeChange nc : transactionLog.get(n)) {
                list.add(nc);
            }
        }
        return list;
    }

    private LinkedHashMap<StateNode, List<NodeChange>> getOptimizedTransactionLog(
            LinkedHashMap<StateNode, List<NodeChange>> transactionLog) {
        TransactionLogOptimizer optimizer = new TransactionLogOptimizer(null,
                transactionLog, new HashSet<>());

        return optimizer.getChanges();
    }

    private LinkedHashMap<StateNode, List<NodeChange>> getTransactionLog(
            RootNode root) {
        TransactionLogBuilder logBuilder = new TransactionLogBuilder();
        root.commit(logBuilder.getVisitor());
        return logBuilder.getChanges();
    }

    private void commit() {
        transactionLog = getTransactionLog(root);
        transactionLogSize = flatten(transactionLog).size();

        // Make a copy so the original transactionLog is available for testing
        optimizedTransactionLog = getOptimizedTransactionLog(
                clone(transactionLog));
        optimizedTransactionLogSize = flatten(optimizedTransactionLog).size();
    }

    private LinkedHashMap<StateNode, List<NodeChange>> clone(
            LinkedHashMap<StateNode, List<NodeChange>> original) {
        LinkedHashMap<StateNode, List<NodeChange>> clone = new LinkedHashMap<>();
        for (StateNode node : original.keySet()) {
            clone.put(node, new ArrayList<>(original.get(node)));
        }
        return clone;
    }

}
