package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChange;
import com.vaadin.hummingbird.kernel.change.NodeChangeVisitor;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;

public class StateNodeTest {

    private static final String key = "key";

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
        assert false;
    }

    @Test
    public void addSingleValue() {
        StateNode node = StateNode.create();
        StateNode child = StateNode.create();

        node.put(key, child);

        Assert.assertSame(child.getParent(), node);
        Assert.assertSame(child, node.get(key));
    }

    @Test
    public void addMultipleItems() {
        StateNode node = StateNode.create();
        StateNode child = StateNode.create();

        List<Object> list = node.getMultiValued(key);

        list.add(0, child);

        Assert.assertEquals(1, list.size());
        Assert.assertSame(child, list.get(0));
        Assert.assertSame(node, child.getParent());

        list.add(1, "value 2");

        Assert.assertEquals(2, list.size());
        Assert.assertSame(child, list.get(0));
        Assert.assertEquals("value 2", list.get(1));

        list.remove(child);
        Assert.assertNull(child.getParent());
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("value 2", list.get(0));
    }

    @Test
    public void getSingleItem_noValue_null() {
        StateNode node = StateNode.create();

        Object value = node.get(key);
        Assert.assertNull(value);
    }

    @Test
    public void replaceSingleValue_unregister() {
        StateNode node = StateNode.create();
        StateNode child = StateNode.create();

        node.put(key, child);

        // Sanity check
        Assert.assertSame(node, child.getParent());

        node.put(key, "Value");

        Assert.assertNull(child.getParent());
    }

    @Test
    public void replaceListValue_unregister() {
        StateNode node = StateNode.create();
        StateNode child = StateNode.create();

        List<Object> list = node.getMultiValued(key);

        list.add(0, child);

        // Sanity check
        Assert.assertSame(node, child.getParent());

        list.set(0, "Value");

        Assert.assertNull(child.getParent());
    }

    @Test
    public void getSingleValueTyped_rightType() {
        StateNode node = StateNode.create();
        node.put(key, "Value");

        String value = node.get(key, String.class);
        // All is fine if we get past this point

        Assert.assertEquals("Value", value);
    }

    @Test(expected = AssertionError.class)
    public void getSingleValueTyped_wrongType() {
        StateNode node = StateNode.create();
        node.put(key, "Value");

        node.get(key, Number.class);
    }

    @Test
    public void getSingleValueTyped_null() {
        StateNode node = StateNode.create();

        String value = node.get(key, String.class);
        // All is fine if we get past this point

        Assert.assertNull(value);
    }

    @Test
    public void attachedNodes() {
        RootNode rootNode = new RootNode();

        StateNode childNode = StateNode.create();

        Assert.assertSame(rootNode, rootNode.getRoot());
        Assert.assertNull(childNode.getRoot());

        Assert.assertEquals(1, rootNode.getId());
        Assert.assertEquals(0, childNode.getId());

        rootNode.put("child", childNode);

        Assert.assertSame(rootNode, childNode.getRoot());
        Assert.assertEquals(2, childNode.getId());
        Assert.assertSame(childNode, rootNode.getById(2));
    }

    @Test
    public void attachNodesInChildren() {
        List<StateNode> nodes = createNodeHierarchy();

        for (StateNode n : nodes) {
            Assert.assertNull(n.getRoot());
        }

        RootNode root = new RootNode();
        root.put("child", nodes.get(0));

        for (StateNode n : nodes) {
            Assert.assertSame(root, n.getRoot());
        }
    }

    private static List<StateNode> createNodeHierarchy() {
        StateNode n1 = StateNode.create();
        StateNode n2 = StateNode.create();
        StateNode n3 = StateNode.create();

        List<Object> list = n1.getMultiValued(key);

        n1.put("child", n2);
        list.add(n3);

        return Arrays.asList(n1, n2, n3);
    }

    @Test
    public void detachAttached() {
        List<StateNode> nodes = createNodeHierarchy();
        StateNode n1 = nodes.get(0);

        RootNode root = new RootNode();
        root.put("child", n1);

        for (int i = 0; i < nodes.size(); i++) {
            StateNode n = nodes.get(i);
            int id = n.getId();
            n.put("id", Integer.valueOf(id));

            Assert.assertSame(root, n.getRoot());
            Assert.assertSame(n, root.getById(id));
        }

        root.remove("child");
        for (int i = 0; i < nodes.size(); i++) {
            StateNode n = nodes.get(i);
            Assert.assertSame(root, n.getRoot());
            Assert.assertNull(root.getById(n.getId()));
        }

        root.put("child", n1);
        for (int i = 0; i < nodes.size(); i++) {
            StateNode n = nodes.get(i);
            Assert.assertEquals(n.get("id"), Integer.valueOf(n.getId()));
            Assert.assertSame(root, n.getRoot());
            Assert.assertSame(n, root.getById(n.getId()));
        }
    }

    @Test
    public void testSimpleTransaction() {
        RootNode root = new RootNode();

        root.put("1", "1");
        Assert.assertEquals("1", root.get("1"));

        root.commit();
        Assert.assertEquals("1", root.get("1"));

        root.put("2", "2");
        Assert.assertEquals("2", root.get("2"));

        root.commit();
        root.put("3", "3");
        Assert.assertEquals("3", root.get("3"));
        root.rollback();

        Assert.assertEquals("1", root.get("1"));
        Assert.assertEquals("2", root.get("2"));
        Assert.assertFalse(root.containsKey("3"));
    }

    @Test
    public void transactionalRemoves_committed() {
        RootNode root = new RootNode();

        root.put("1", "1");

        root.commit();

        root.remove("1");
        root.put("2", "2");

        Assert.assertFalse(root.containsKey("1"));
        Assert.assertEquals("2", root.get("2"));

        Set<String> stringKeys = root.getStringKeys();
        Assert.assertEquals(1, stringKeys.size());
        Assert.assertTrue(stringKeys.contains("2"));

        root.commit();

        Assert.assertFalse(root.containsKey("1"));
        Assert.assertEquals("2", root.get("2"));

        stringKeys = root.getStringKeys();
        Assert.assertEquals(1, stringKeys.size());
        Assert.assertTrue(stringKeys.contains("2"));
    }

    @Test
    public void transactionalRemoves_rolledBack() {
        RootNode root = new RootNode();

        root.put("1", "1");

        root.commit();

        root.remove("1");
        root.put("2", "2");

        Assert.assertFalse(root.containsKey("1"));
        Assert.assertEquals("2", root.get("2"));

        Set<String> stringKeys = root.getStringKeys();
        Assert.assertEquals(1, stringKeys.size());
        Assert.assertTrue(stringKeys.contains("2"));

        root.rollback();

        Assert.assertFalse(root.containsKey("2"));
        Assert.assertEquals("1", root.get("1"));

        stringKeys = root.getStringKeys();
        Assert.assertEquals(1, stringKeys.size());
        Assert.assertTrue(stringKeys.contains("1"));
    }

    @Test
    public void transactionalAttachDetach_rollback() {
        RootNode root = new RootNode();
        StateNode n1 = StateNode.create();
        StateNode n2 = StateNode.create();
        StateNode n3 = StateNode.create();

        root.put("child", n1);
        int n1Id = n1.getId();

        root.commit();

        root.put("child", n2);
        root.put("another", n3);

        int n2Id = n2.getId();
        int n3Id = n3.getId();

        Assert.assertSame(n2, root.get("child"));

        Assert.assertFalse(n1.isAttached());
        Assert.assertTrue(n2.isAttached());
        Assert.assertTrue(n3.isAttached());

        Assert.assertNull(n1.getParent());
        Assert.assertSame(root, n2.getParent());
        Assert.assertSame(root, n3.getParent());

        Assert.assertNull(root.getById(n1Id));
        Assert.assertSame(n2, root.getById(n2Id));
        Assert.assertSame(n3, root.getById(n3Id));

        root.rollback();

        Assert.assertSame(n1, root.get("child"));

        Assert.assertTrue(n1.isAttached());
        Assert.assertFalse(n2.isAttached());
        Assert.assertFalse(n3.isAttached());

        Assert.assertSame(root, n1.getParent());
        Assert.assertNull(n2.getParent());
        Assert.assertNull(n3.getParent());

        Assert.assertSame(n1, root.getById(n1Id));
        Assert.assertNull(root.getById(n2Id));
        Assert.assertNull(root.getById(n3Id));
    }

    @Test
    public void transactionalAttachDetach_commit() {
        RootNode root = new RootNode();
        StateNode n1 = StateNode.create();
        StateNode n2 = StateNode.create();
        StateNode n3 = StateNode.create();

        root.put("child", n1);
        int n1Id = n1.getId();

        root.commit();

        root.put("child", n2);
        root.put("another", n3);

        int n2Id = n2.getId();
        int n3Id = n3.getId();

        Assert.assertSame(n2, root.get("child"));

        Assert.assertFalse(n1.isAttached());
        Assert.assertTrue(n2.isAttached());
        Assert.assertTrue(n3.isAttached());

        Assert.assertNull(n1.getParent());
        Assert.assertSame(root, n2.getParent());
        Assert.assertSame(root, n3.getParent());

        Assert.assertNull(root.getById(n1Id));
        Assert.assertSame(n2, root.getById(n2Id));
        Assert.assertSame(n3, root.getById(n3Id));

        root.commit();

        Assert.assertSame(n2, root.get("child"));

        Assert.assertFalse(n1.isAttached());
        Assert.assertTrue(n2.isAttached());
        Assert.assertTrue(n3.isAttached());

        Assert.assertNull(n1.getParent());
        Assert.assertSame(root, n2.getParent());
        Assert.assertSame(root, n3.getParent());

        Assert.assertNull(root.getById(n1Id));
        Assert.assertSame(n2, root.getById(n2Id));
        Assert.assertSame(n3, root.getById(n3Id));
    }

    @Test(expected = IllegalStateException.class)
    public void attachAlreadyAttached_throw() {
        StateNode parent1 = StateNode.create();
        StateNode parent2 = StateNode.create();
        StateNode child = StateNode.create();

        parent1.put(key, child);
        parent2.put(key, child);
    }

    @Test(expected = IllegalStateException.class)
    public void attachToCurrentOwner_throw() {
        StateNode parent = StateNode.create();
        StateNode child = StateNode.create();

        parent.put(key, child);
        parent.put("other", child);
    }

    @Test
    public void rollbackUnattachdedValues() {
        RootNode root = new RootNode();
        StateNode child = StateNode.create();
        child.put(key, "value");

        root.put(key, child);
        root.rollback();

        Assert.assertNull(child.get(key));
    }

    @Test
    public void stateListRollback() {
        RootNode rootNode = new RootNode();
        StateNode child = StateNode.create();

        List<Object> list = rootNode.getMultiValued(key);

        list.add("Lorem");
        list.add(child);
        list.add("Ipsum");

        List<Object> listCopy = new ArrayList<>(list);

        rootNode.commit();

        list.remove(0);
        list.add(1, "Blorem");
        list.set(0, "Lispum");

        rootNode.rollback();

        Assert.assertEquals(listCopy, new ArrayList<>(list));
    }

    @Test
    public void testCommitLog() {
        RootNode rootNode = new RootNode();
        StateNode child = StateNode.create();

        Map<Object, Object> rootMap = new HashMap<>();
        List<Object> rootList = new ArrayList<>();
        rootNode.addCommitVisitor(new NodeChangeVisitor() {
            @Override
            public void visitRemoveChange(StateNode node,
                    RemoveChange removeChange) {
                Assert.assertSame(rootNode, node);
                rootMap.remove(removeChange.getKey());
            }

            @Override
            public void visitPutChange(StateNode node, PutChange putChange) {
                Assert.assertSame(rootNode, node);
                rootMap.put(putChange.getKey(), putChange.getValue());
            }

            @Override
            public void visitParentChange(StateNode node,
                    ParentChange parentChange) {
                Assert.assertSame(child, node);
                Assert.assertSame(rootNode, parentChange.getNewParent());
            }

            @Override
            public void visitListReplaceChange(StateNode node,
                    ListReplaceChange listReplaceChange) {
                Assert.assertEquals("list", listReplaceChange.getKey());
                Assert.assertSame(rootNode, node);
                rootList.set(listReplaceChange.getIndex(),
                        listReplaceChange.getNewValue());
            }

            @Override
            public void visitListRemoveChange(StateNode node,
                    ListRemoveChange listRemoveChange) {
                Assert.assertEquals("list", listRemoveChange.getKey());
                Assert.assertSame(rootNode, node);
                rootList.remove(listRemoveChange.getIndex());
            }

            @Override
            public void visitListInsertChange(StateNode node,
                    ListInsertChange listInsertChange) {
                Assert.assertEquals("list", listInsertChange.getKey());
                Assert.assertSame(rootNode, node);
                rootList.add(listInsertChange.getIndex(),
                        listInsertChange.getValue());
            }

            @Override
            public void visitIdChange(StateNode node, IdChange idChange) {
                if (node == rootNode) {
                    Assert.assertEquals(rootNode.getId(), idChange.getNewId());
                } else if (node == child) {
                    Assert.assertEquals(node.getId(), idChange.getNewId());
                } else {
                    Assert.fail();
                }
            }
        });

        rootNode.put(key, "value");
        rootNode.put(key, "replacement");
        rootNode.remove(key);
        rootNode.put(key, child);

        List<Object> listView = rootNode.getMultiValued("list");
        listView.add("value");
        listView.add(0, "additional");
        listView.set(0, "replacement");
        listView.remove(0);

        rootNode.commit();

        Assert.assertEquals(1, rootMap.size());
        Assert.assertSame(child, rootMap.get(key));

        Assert.assertEquals(1, rootList.size());
        Assert.assertEquals("value", rootList.get(0));
    }

    @Test
    public void replaceAndReuseList() {
        StateNode node = StateNode.create();
        StateNode child = StateNode.create();

        List<Object> listView = node.getMultiValued(key);
        listView.add(child);

        Assert.assertSame(node, child.getParent());

        node.put(key, "foo");

        Assert.assertEquals("foo", node.get(key));
        Assert.assertNull(child.getParent());
    }

    @Test
    public void detachedListThrows() {
        StateNode stateNode = StateNode.create();

        List<Object> asList = stateNode.getMultiValued(key);
        stateNode.put(key, "foo");

        try {
            asList.size();
            Assert.fail();
        } catch (IllegalStateException expected) {
            // All is OK
        }

        try {
            asList.get(0);
            Assert.fail();
        } catch (IllegalStateException expected) {
            // All is OK
        }

        try {
            asList.add("bar");
            Assert.fail();
        } catch (IllegalStateException expected) {
            // All is OK
        }

        try {
            asList.set(0, "baz");
            Assert.fail();
        } catch (IllegalStateException expected) {
            // All is OK
        }

        try {
            asList.remove(0);
            Assert.fail();
        } catch (IllegalStateException expected) {
            // All is OK
        }
    }

    @Test
    public void getAsList_sameInstance() {
        StateNode node = StateNode.create();
        List<Object> listView1 = node.getMultiValued(key);
        List<Object> listView2 = node.getMultiValued(key);

        listView1.add("foo");

        Assert.assertEquals(1, listView2.size());
        Assert.assertEquals("foo", listView2.get(0));
    }

    @Test
    public void testClassBackedNode() {
        Object customKey = new Object();

        Map<Object, Class<?>> fields = new HashMap<>();
        fields.put("string", String.class);
        fields.put(customKey, int.class);

        StateNode node = StateNode.create(fields);

        Assert.assertSame(String.class, node.getType("string"));
        Assert.assertSame(int.class, node.getType(customKey));
        Assert.assertSame(Object.class, node.getType("genericKey"));

        node.setValue("string", "My string");
        node.setValue(customKey, Integer.valueOf(5));

        Assert.assertEquals("My string", node.get("string"));
        Assert.assertEquals(Integer.valueOf(5), node.get(customKey));
    }

    @Test
    public void backedNode_sameDefinition_sameClass() {
        StateNode node1 = StateNode.create(new HashMap<Object, Class<?>>() {
            {
                put("string", String.class);
            }
        });

        StateNode node2 = StateNode
                .create(Collections.singletonMap("string", String.class));

        Assert.assertSame(node1.getClass(), node2.getClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void backedNode_invalidType_throws() {
        StateNode node = StateNode
                .create(Collections.singletonMap("string", String.class));
        node.put("string", Integer.valueOf(5));
    }

    @Test
    public void backedNodeAlwaysContainsKey() {
        StateNode node = StateNode
                .create(Collections.singletonMap("string", String.class));

        Assert.assertTrue(node.containsKey("string"));
        Assert.assertFalse(node.containsKey("someOtherKey"));

        Set<String> stringKeys = node.getStringKeys();
        Assert.assertEquals(1, stringKeys.size());
        Assert.assertEquals("string", stringKeys.iterator().next());
    }

    @Test(expected = IllegalArgumentException.class)
    public void backedNodeRemoveThrows() {
        StateNode node = StateNode
                .create(Collections.singletonMap("string", String.class));
        node.remove("string");
    }

    @Test
    public void backedNode_challengingKeys() {
        List<Object> keys = new ArrayList<>();

        // Name shadowing candidates
        keys.addAll(Arrays.asList("id", "parent", "fieldMap", "values"));

        // Illegal java identifier
        keys.add("class");

        // Duplicate toString value
        keys.addAll(Arrays.asList("string", new Object() {
            @Override
            public String toString() {
                return "string";
            }
        }));

        Map<Object, Class<?>> fields = keys.stream()
                .collect(Collectors.toMap(key -> key, key -> String.class));

        StateNode node = StateNode.create(fields);

        for (int i = 0; i < keys.size(); i++) {
            node.put(keys.get(i), Integer.toString(i));
        }

        for (int i = 0; i < keys.size(); i++) {
            Assert.assertEquals(Integer.toString(i), node.get(keys.get(i)));
        }
    }

    @Test
    public void simpleNodeChangeListener() {
        RootNode root = new RootNode();
        StateNode node = StateNode.create();
        root.put(StateNode.class, node);

        List<List<NodeChange>> recordedChanges = new ArrayList<>();

        node.addChangeListener(new NodeChangeListener() {
            @Override
            public void onChange(StateNode stateNode,
                    List<NodeChange> changes) {
                recordedChanges.add(new ArrayList<>(changes));
            }
        });

        // Flush old setup changes
        root.commit();
        recordedChanges.clear();

        node.put("value", "value");

        Assert.assertEquals(0, recordedChanges.size());

        root.commit();

        Assert.assertEquals(1, recordedChanges.size());

        List<NodeChange> changes = recordedChanges.get(0);
        Assert.assertEquals(1, changes.size());
    }

    @Test
    public void nodeChangeListenerCountUpdate() {
        RootNode root = new RootNode();
        StateNode node = StateNode.create();
        root.put(StateNode.class, node);

        ArrayList<Integer> changesCounts = new ArrayList<>();

        node.addChangeListener(new NodeChangeListener() {
            @Override
            public void onChange(StateNode stateNode,
                    List<NodeChange> changes) {
                changesCounts.add(Integer.valueOf(changes.size()));

                List<Object> list = stateNode.getMultiValued("list");
                Integer count = stateNode.get("count", Integer.class);
                if (count == null || count.intValue() != list.size()) {
                    stateNode.put("count", Integer.valueOf(list.size()));
                }
            }
        });

        List<Object> multiValued = node.getMultiValued("list");
        multiValued.add("");

        Assert.assertNull(node.get("count"));
        Assert.assertEquals(0, changesCounts.size());

        root.commit();

        Assert.assertEquals(Integer.valueOf(1), node.get("count"));
        Assert.assertEquals(2, changesCounts.size());

        // IdChange, ParentChange, ListInsert (listeners), ListInsert (list)
        Assert.assertEquals(Integer.valueOf(4), changesCounts.get(0));
        // PutChange (count)
        Assert.assertEquals(Integer.valueOf(1), changesCounts.get(1));
    }
}
