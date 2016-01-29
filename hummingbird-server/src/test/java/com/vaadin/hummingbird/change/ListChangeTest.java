package com.vaadin.hummingbird.change;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateNodeTest;
import com.vaadin.hummingbird.namespace.AbstractNamespaceTest;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ListNamespace;
import com.vaadin.hummingbird.namespace.NamespaceRegistry;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class ListChangeTest {
    private ListNamespace namespace = AbstractNamespaceTest
            .createNamespace(ElementChildrenNamespace.class);

    @Test
    public void testBasicJson() {
        StateNode child1 = StateNodeTest.createEmptyNode("child1");
        StateNode child2 = StateNodeTest.createEmptyNode("child2");
        ListSpliceChange change = new ListSpliceChange(namespace, 0, 1,
                Arrays.asList(child1, child2));

        JsonObject json = change.toJson();

        Assert.assertEquals(change.getNode().getId(),
                (int) json.getNumber("node"));
        Assert.assertEquals(NamespaceRegistry.getId(namespace.getClass()),
                (int) json.getNumber("ns"));
        Assert.assertEquals("splice", json.getString("type"));
        Assert.assertEquals(0, (int) json.getNumber("index"));
        Assert.assertEquals(1, (int) json.getNumber("remove"));

        JsonArray addNodes = json.getArray("addNodes");
        Assert.assertEquals(2, addNodes.length());

        Assert.assertEquals(child1.getId(), (int) addNodes.getNumber(0));
        Assert.assertEquals(child2.getId(), (int) addNodes.getNumber(1));
    }

    @Test
    public void testZeroRemoveNotInJson() {
        ListSpliceChange change = new ListSpliceChange(namespace, 1, 0,
                Arrays.asList());

        JsonObject json = change.toJson();

        Assert.assertFalse(json.hasKey("remove"));
    }

    @Test
    public void testEmptyAddNotInJson() {
        ListSpliceChange change = new ListSpliceChange(namespace, 1, 0,
                Arrays.asList());

        JsonObject json = change.toJson();

        Assert.assertFalse(json.hasKey("addNodes"));
    }
}
