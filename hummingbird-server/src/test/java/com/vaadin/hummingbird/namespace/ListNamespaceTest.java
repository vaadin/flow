package com.vaadin.hummingbird.namespace;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.change.ListSpliceChange;
import com.vaadin.hummingbird.change.NodeChange;

public class ListNamespaceTest
        extends AbstractNamespaceTest<ElementChildrenNamespace> {
    private ListNamespace namespace = createNamespace();

    @Test
    public void testAddingAndRemoving() {
        Object value1 = new Object();
        Object value2 = new Object();

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
        Object value1 = new Object();
        Object value2 = new Object();

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
}
