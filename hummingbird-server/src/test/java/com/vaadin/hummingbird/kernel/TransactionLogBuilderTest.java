package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.vaadin.hummingbird.kernel.StateNode.DataProvider;
import com.vaadin.hummingbird.kernel.StateNode.LazyList;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListInsertManyChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeContentsChange;
import com.vaadin.hummingbird.kernel.change.NodeDataChange;
import com.vaadin.hummingbird.kernel.change.NodeListChange;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RangeEndChange;
import com.vaadin.hummingbird.kernel.change.RangeStartChange;
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
        assertOptimizedChanges(
                new ListInsertManyChange(0, LIST_KEY, node2, node3));

    }

    @Test
    public void listAddFiveToInitializedList() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(new DebugStateNode("pre-created1"));
        list.add(new DebugStateNode("pre-created2"));
        list.add(new DebugStateNode("pre-created3"));
        commit();
        list.add(node1);
        list.add(node2);
        list.add(node3);
        list.add(node4);
        list.add(node5);

        commit();

        assertOptimizedChanges(new ListInsertManyChange(3, LIST_KEY, node1,
                node2, node3, node4, node5));

    }

    @Test
    public void listAddAddNonEmptyList() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        commit();

        list.add(node2);
        list.add(node3);
        commit();

        assertOptimizedChanges(
                new ListInsertManyChange(1, LIST_KEY, node2, node3));

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
        } else if (change1.getClass() == ListInsertManyChange.class) {
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
        } else if (change1.getClass() == RangeEndChange.class) {
            assertChangesEquals((RangeEndChange) change1,
                    (RangeEndChange) change2);
        } else if (change1.getClass() == RangeStartChange.class) {
            assertChangesEquals((RangeStartChange) change1,
                    (RangeStartChange) change2);
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
        Assert.assertEquals("Unexpected index for change", change1.getIndex(),
                change2.getIndex());
        Assert.assertEquals(change1.getValue(), change2.getValue());
    }

    private void assertChangesEquals(NodeContentsChange change1,
            NodeContentsChange change2) {
        Assert.assertEquals(change1.getKey(), change2.getKey());
    }

    private void assertChangesEquals(RangeStartChange change1,
            RangeStartChange change2) {
        Assert.assertEquals(change1.getKey(), change2.getKey());
        Assert.assertEquals(change1.getRangeStart(), change2.getRangeStart());
    }

    private void assertChangesEquals(RangeEndChange change1,
            RangeEndChange change2) {
        Assert.assertEquals(change1.getKey(), change2.getKey());
        Assert.assertEquals(change1.getRangeEnd(), change2.getRangeEnd());
    }

    private void assertChangesEquals(NodeDataChange change1,
            NodeDataChange change2) {
        assertChangesEquals((NodeContentsChange) change1,
                (NodeContentsChange) change2);
        if (!"IGNORE".equals(change1.getValue())
                && !"IGNORE".equals(change2.getValue())) {
            Assert.assertEquals(change1.getValue(), change2.getValue());
        }
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
    public void listAddReplaceAdd() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.set(0, node2);
        list.add(node3);

        commit();

        Assert.assertFalse(
                "No changes related to the removed node should be in the optimized log",
                optimizedLogContainsNode(node1));
        assertOptimizedChanges(
                new ListInsertManyChange(0, LIST_KEY, node2, node3));
    }

    @Test
    public void listAddReplace() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.set(0, node2);

        commit();

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
        // TODO Optimize out-of-order adds
        // assertOptimizedChanges(new ListInsertManyChange(0, LIST_KEY, node4,
        // node1, node2, node5));

        assertOptimizedChanges(
                new ListInsertManyChange(0, LIST_KEY, node1, node2),
                new ListInsertChange(0, LIST_KEY, node4),
                new ListInsertChange(3, LIST_KEY, node5));
    }

    @Test
    public void listAddOutOfOrder() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.add(node2);
        // Insert between two existing
        list.add(1, node3);

        commit();

        // TODO Should optimize no matter the order of the inserts
        assertOptimizedChanges(
                new ListInsertManyChange(0, LIST_KEY, node1, node2),
                new ListInsertChange(1, LIST_KEY, node3));

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
    public void listAddAfterCommit() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.add(node2);
        commit();
        list.add(1, node3);
        commit();
        assertOptimizedChanges(new ListInsertChange(1, LIST_KEY, node3));
    }

    @Test
    public void listRemoveAfterCommit() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.add(node2);
        commit();
        list.remove(0);
        commit();
        assertOptimizedChanges(new ListRemoveChange(0, LIST_KEY, node1));
    }

    @Test
    public void listRemovesAfterCommit() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.add(node2);
        commit();
        list.remove(0);
        list.remove(0);
        commit();
        // This could be optimized to be one operation
        assertOptimizedChanges(new ListRemoveChange(0, LIST_KEY, node1),
                new ListRemoveChange(0, LIST_KEY, node2));
    }

    @Test
    public void listRemoveAddAfterCommit() {
        List<Object> list = node.getMultiValued(LIST_KEY);
        list.add(node1);
        list.add(node2);
        commit();
        list.remove(0);
        list.add(0, node1);
        commit();
        // This could be optimized to be a no-op
        assertOptimizedChanges(new ListRemoveChange(0, LIST_KEY, node1),
                new ListInsertChange(0, LIST_KEY, node1));
    }

    @Test
    public void putPutShouldBeOnePut() {
        node.put("foo", "bar");
        node.put("foo", "baz");
        commit();
        Assert.assertEquals(1, optimizedTransactionLogSize);
        assertOptimizedChanges(new PutChange("foo", "baz"));

    }

    @Test
    public void putRemovePutShouldBeOnePut() {
        node.put("foo", "bar");
        node.remove("foo");
        node.put("foo", "baz");
        commit();
        Assert.assertEquals(1, optimizedTransactionLogSize);
        assertOptimizedChanges(new PutChange("foo", "baz"));

    }

    @Test
    public void putRemoveShouldBeRemove() {
        node.put("foo", "bar");
        node.remove("foo");
        commit();
        assertOptimizedChanges(new RemoveChange("foo", "bar"));

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

    public static LinkedHashMap<StateNode, List<NodeChange>> getOptimizedTransactionLog(
            LinkedHashMap<StateNode, List<NodeChange>> transactionLog) {
        TransactionLogOptimizer optimizer = new TransactionLogOptimizer(null,
                transactionLog, new HashSet<>());

        return optimizer.getChanges();
    }

    public static LinkedHashMap<StateNode, List<NodeChange>> getTransactionLog(
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

    @Test
    public void addRemoveAddList() {
        List<Object> l = node.getMultiValued(LIST_KEY);
        l.add("bar");
        l.remove("bar");
        node.remove(LIST_KEY);
        l = node.getMultiValued(LIST_KEY);
        l.add("foo");
        commit();
        // TODO #36, this should only be a ListInsertChange
        assertOptimizedChanges(new RemoveChange(LIST_KEY, "IGNORE"),
                new ListInsertChange(0, LIST_KEY, "foo"));
    }

    @Test
    public void basicLazyList() {
        node.put(LIST_KEY, LazyList.create(new DataProvider<SimpleBean>() {

            @Override
            public List<SimpleBean> getValues(int index, int count) {
                Assert.fail("Should not fetch row without active range");
                return null;
            }

            @Override
            public Class<SimpleBean> getType() {
                return SimpleBean.class;
            }
        }));
        commit();
        assertOptimizedChanges();
    }

    @Test
    public void basicLazyListRangeChange() {
        node.put(LIST_KEY, LazyList.create(new DataProvider<SimpleBean>() {

            @Override
            public List<SimpleBean> getValues(int index, int count) {
                List<SimpleBean> l = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    l.add(new SimpleBean("Value " + (index + i)));
                }
                return l;
            }

            @Override
            public Class<SimpleBean> getType() {
                return SimpleBean.class;
            }
        }));
        LazyList<StateNode> ll = node.get(LIST_KEY, LazyList.class);
        ll.setActiveRangeEnd(110);
        ll.setActiveRangeStart(100);
        commit();

        // Should have rangeStart:100, rangeEnd:100, create 10 data nodes,
        // insert nodes into the list
        Assert.assertEquals(optimizedTransactionLogSize, 1 + 1 + 10 + 1);
        assertChangesEquals(new RangeEndChange(LIST_KEY, 110),
                (RangeEndChange) flatten(optimizedTransactionLog).get(0));
        assertChangesEquals(new RangeStartChange(LIST_KEY, 100),
                (RangeStartChange) flatten(optimizedTransactionLog).get(1));
                // assertOptimizedChanges(,

        // Insert nodes
        ListInsertManyChange insert = (ListInsertManyChange) flatten(
                optimizedTransactionLog).get(2);
        Assert.assertEquals(LIST_KEY, insert.getKey());
        Assert.assertEquals(0, insert.getIndex());
        Assert.assertEquals(10, insert.getValue().size()); // ListInsert
                                                           // contains 10 nodes
        for (int i = 0; i < 10; i++) {
            PutChange putChange = (PutChange) flatten(optimizedTransactionLog)
                    .get(3 + i);
            assertChangesEquals(new PutChange("value", "Value " + (100 + i)),
                    putChange); // The value should match the generated one

            StateNode putNode = getNodeForChange(putChange);

            StateNode insertNode = ((StateNode) ((List) insert.getValue())
                    .get(i));
            Assert.assertSame(insertNode, putNode); // Node in listinsert should
                                                    // be the same as in the put
                                                    // which sets the value
        }

    }

    private StateNode getNodeForChange(NodeChange change) {
        for (Entry<StateNode, List<NodeChange>> entry : optimizedTransactionLog
                .entrySet()) {
            if (entry.getValue().contains(change)) {
                return entry.getKey();
            }
        }
        return null;

    }

    @Test
    public void basicLazyListRangeChangeAfterCommit() {
        node.put(LIST_KEY, LazyList.create(new DataProvider<SimpleBean>() {

            @Override
            public List<SimpleBean> getValues(int index, int count) {
                List<SimpleBean> l = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    l.add(new SimpleBean("Value " + (index + i)));
                }
                return l;
            }

            @Override
            public Class<SimpleBean> getType() {
                return SimpleBean.class;
            }
        }));

        LazyList<StateNode> ll = node.get(LIST_KEY, LazyList.class);
        commit();
        ll.setActiveRangeEnd(120);
        ll.setActiveRangeStart(90);
        ll.setActiveRangeEnd(105);
        ll.setActiveRangeStart(100);
        commit();

        // Should have rangeStart:100, rangeEnd:105, create 5 data nodes,
        // insert nodes into the list
        Assert.assertEquals(optimizedTransactionLogSize, 1 + 1 + 5 + 1);
        assertChangesEquals(new RangeEndChange(LIST_KEY, 105),
                (RangeEndChange) flatten(optimizedTransactionLog).get(0));

        assertChangesEquals(new RangeStartChange(LIST_KEY, 100),
                (RangeStartChange) flatten(optimizedTransactionLog).get(1));
        // assertOptimizedChanges(,
        // Insert nodes
        ListInsertManyChange insert = (ListInsertManyChange) flatten(
                optimizedTransactionLog).get(2);
        Assert.assertEquals(LIST_KEY, insert.getKey());
        Assert.assertEquals(0, insert.getIndex());
        Assert.assertEquals(5, insert.getValue().size()); // ListInsert
                                                          // contains 10 nodes
        for (int i = 0; i < 5; i++) {
            PutChange putChange = (PutChange) flatten(optimizedTransactionLog)
                    .get(3 + i);
            assertChangesEquals(new PutChange("value", "Value " + (100 + i)),
                    putChange); // The value should match the generated one

            StateNode putNode = getNodeForChange(putChange);

            StateNode insertNode = ((StateNode) ((List) insert.getValue())
                    .get(i));
            Assert.assertSame(insertNode, putNode); // Node in listinsert should
                                                    // be the same as in the put
                                                    // which sets the value
        }
    }

}
