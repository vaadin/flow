/*
 * Copyright 2000-2016 Vaadin Ltd.
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

package com.vaadin.hummingbird;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.change.NodeAttachChange;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.change.NodeDetachChange;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;
import com.vaadin.hummingbird.namespace.ElementPropertyNamespace;
import com.vaadin.hummingbird.namespace.Namespace;

public class StateNodeTest {
    @Test
    public void newNodeState() {
        StateNode node = createEmptyNode();

        NodeOwner owner = node.getOwner();

        Assert.assertNotNull("New node should have an owner", owner);

        Assert.assertEquals("New node shold have unassigned id", -1,
                node.getId());

        Assert.assertFalse("Node should not be attached", node.isAttached());
    }

    @Test
    public void nodeContainsDefinedNamespaces() {
        StateNode node = new StateNode(ElementDataNamespace.class);

        Assert.assertTrue("Should have namespace defined in constructor",
                node.hasNamespace(ElementDataNamespace.class));

        ElementDataNamespace namespace = node
                .getNamespace(ElementDataNamespace.class);

        Assert.assertNotNull("Existing namespace should also be available",
                namespace);

        Assert.assertFalse(
                "Should not have namespace that wasn't defined in constructor",
                node.hasNamespace(ElementPropertyNamespace.class));
    }

    @Test(expected = IllegalStateException.class)
    public void getMissingNamespaceThrows() {
        StateNode node = new StateNode(ElementDataNamespace.class);
        node.getNamespace(ElementPropertyNamespace.class);
    }

    @Test
    public void testAttachDetachChangeCollection() {
        StateNode node = createEmptyNode();

        List<NodeChange> changes = new ArrayList<>();
        Consumer<NodeChange> collector = changes::add;

        node.collectChanges(collector);

        Assert.assertTrue("Node should have no changes", changes.isEmpty());

        // Attach node
        setParent(node,
                new StateTree(ElementChildrenNamespace.class).getRootNode());

        node.collectChanges(collector);

        Assert.assertEquals("Should have 1 change", 1, changes.size());
        Assert.assertTrue("Should have attach change",
                changes.get(0) instanceof NodeAttachChange);
        changes.clear();

        node.collectChanges(collector);
        Assert.assertTrue("Node should have no changes", changes.isEmpty());

        // Detach node
        setParent(node, null);

        node.collectChanges(collector);
        Assert.assertEquals("Should have 1 change", 1, changes.size());
        Assert.assertTrue("Should have detach change",
                changes.get(0) instanceof NodeDetachChange);
        changes.clear();
    }

    @Test
    public void appendChildBeforeParent() {
        StateNode parent = createParentNode("parent");
        StateNode child = createParentNode("child");
        StateNode grandchild = createEmptyNode("grandchild");

        StateNode root = new StateTree(ElementChildrenNamespace.class)
                .getRootNode();

        setParent(grandchild, child);
        setParent(child, parent);
        setParent(parent, root);

        Assert.assertNotEquals(-1, parent.getId());
        Assert.assertNotEquals(-1, child.getId());
        Assert.assertNotEquals(-1, grandchild.getId());
    }

    @Test
    public void appendParentBeforeChild() {
        StateNode parent = createParentNode("parent");
        StateNode child = createParentNode("child");
        StateNode grandchild = createEmptyNode("grandchild");

        StateNode root = new StateTree(ElementChildrenNamespace.class)
                .getRootNode();

        setParent(parent, root);
        setParent(child, parent);
        setParent(grandchild, child);

        Assert.assertNotEquals(-1, parent.getId());
        Assert.assertNotEquals(-1, child.getId());
        Assert.assertNotEquals(-1, grandchild.getId());
    }

    @Test(expected = IllegalStateException.class)
    public void setChildAsParent() {
        StateNode parent = createParentNode("parent");
        StateNode child = createParentNode("child");

        setParent(child, parent);
        setParent(parent, child);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetAsOwnParent() {
        StateNode parent = createParentNode("parent");

        setParent(parent, parent);
    }

    public static StateNode createEmptyNode() {
        return createEmptyNode("Empty node");
    }

    public static StateNode createEmptyNode(String toString) {
        return createTestNode(toString);
    }

    public static StateNode createParentNode(String toString) {
        return createTestNode(toString, ElementChildrenNamespace.class);
    }

    @SafeVarargs
    public static StateNode createTestNode(String toString,
            Class<? extends Namespace>... namespaces) {
        return new StateNode(namespaces) {
            @Override
            public String toString() {
                if (toString != null) {
                    return toString;
                } else {
                    return super.toString();
                }
            }
        };
    }

    public static void setParent(StateNode child, StateNode parent) {
        if (parent == null) {
            // Remove child
            parent = child.getParent();

            ElementChildrenNamespace children = parent
                    .getNamespace(ElementChildrenNamespace.class);
            children.remove(children.indexOf(child));
        } else {
            // Add child
            assert child.getParent() == null;

            ElementChildrenNamespace children = parent
                    .getNamespace(ElementChildrenNamespace.class);
            children.add(children.size(), child);
        }
    }
}
