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

package com.vaadin.hummingbird.namespace;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateNodeTest;
import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.change.NodeChange;

public class ListNamespaceTest
        extends AbstractNamespaceTest<ElementChildrenNamespace> {
    private ListNamespace<StateNode> namespace = createNamespace();

    @Test
    public void testAddingAndRemoving() {
        StateNode value1 = StateNodeTest.createEmptyNode("value1");
        StateNode value2 = StateNodeTest.createEmptyNode("value2");

        namespace.add(value1);

        Assert.assertEquals(1, namespace.size());
        Assert.assertSame(value1, namespace.get(0));

        List<NodeChange> firstAddChanges = collectChanges(namespace);
        Assert.assertEquals(1, firstAddChanges.size());
        ListSpliceChange firstAddChange = (ListSpliceChange) firstAddChanges
                .get(0);
        Assert.assertEquals(0, firstAddChange.getIndex());
        Assert.assertEquals(0, firstAddChange.getRemoveCount());
        Assert.assertEquals(Arrays.asList(value1),
                firstAddChange.getNewItems());

        namespace.add(0, value2);
        Assert.assertEquals(2, namespace.size());
        Assert.assertSame(value2, namespace.get(0));
        Assert.assertSame(value1, namespace.get(1));

        List<NodeChange> secondAddChanges = collectChanges(namespace);
        Assert.assertEquals(1, secondAddChanges.size());
        ListSpliceChange secondAddChange = (ListSpliceChange) secondAddChanges
                .get(0);
        Assert.assertEquals(0, secondAddChange.getIndex());
        Assert.assertEquals(0, secondAddChange.getRemoveCount());
        Assert.assertEquals(Arrays.asList(value2),
                secondAddChange.getNewItems());

        namespace.remove(0);

        Assert.assertEquals(1, namespace.size());
        Assert.assertSame(value1, namespace.get(0));

        List<NodeChange> removeChanges = collectChanges(namespace);
        Assert.assertEquals(1, removeChanges.size());
        ListSpliceChange removeChange = (ListSpliceChange) removeChanges.get(0);
        Assert.assertEquals(0, removeChange.getIndex());
        Assert.assertEquals(1, removeChange.getRemoveCount());
        Assert.assertEquals(Arrays.asList(), removeChange.getNewItems());
    }

    @Test
    public void testChangesAfterRest() {
        StateNode value1 = StateNodeTest.createEmptyNode("value1");
        StateNode value2 = StateNodeTest.createEmptyNode("value2");

        namespace.add(value1);
        namespace.add(value2);

        namespace.resetChanges();

        List<NodeChange> changes = collectChanges(namespace);

        Assert.assertEquals(1, changes.size());
        ListSpliceChange change = (ListSpliceChange) changes.get(0);
        Assert.assertEquals(0, change.getIndex());
        Assert.assertEquals(0, change.getRemoveCount());
        Assert.assertEquals(Arrays.asList(value1, value2),
                change.getNewItems());
    }

    @Test
    public void testAttachDetachChildren() {
        StateNode child = StateNodeTest.createEmptyNode("child");

        Assert.assertNull(child.getParent());

        namespace.add(child);

        Assert.assertSame(namespace.getNode(), child.getParent());

        namespace.remove(0);

        Assert.assertNull(child.getParent());
    }

    @Test
    public void testIndexOf() {
        StateNode one = StateNodeTest.createEmptyNode("one");
        StateNode two = StateNodeTest.createEmptyNode("two");
        StateNode three = StateNodeTest.createEmptyNode("three");

        namespace.add(one);
        namespace.add(two);
        Assert.assertEquals(0, namespace.indexOf(one));
        Assert.assertEquals(1, namespace.indexOf(two));
        Assert.assertEquals(-1, namespace.indexOf(three));
    }

    @Test
    public void testClear() {
        StateNode one = StateNodeTest.createEmptyNode("one");
        StateNode two = StateNodeTest.createEmptyNode("two");

        namespace.add(one);
        namespace.add(two);
        Assert.assertEquals(2, namespace.size());
        namespace.clear();
        Assert.assertEquals(0, namespace.size());
    }
}
