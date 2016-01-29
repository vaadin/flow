package com.vaadin.hummingbird.change;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.namespace.AbstractNamespaceTest;
import com.vaadin.hummingbird.namespace.ElementPropertiesNamespace;
import com.vaadin.hummingbird.namespace.MapNamespace;
import com.vaadin.hummingbird.namespace.NamespaceRegistry;

import elemental.json.JsonObject;

public class MapRemoveChangeTest {
    private MapNamespace namespace = AbstractNamespaceTest
            .createNamespace(ElementPropertiesNamespace.class);

    @Test
    public void testJson() {
        MapRemoveChange change = new MapRemoveChange(namespace, "some");

        JsonObject json = change.toJson();

        Assert.assertEquals(change.getNode().getId(),
                (int) json.getNumber("node"));
        Assert.assertEquals(NamespaceRegistry.getId(namespace.getClass()),
                (int) json.getNumber("ns"));
        Assert.assertEquals("remove", json.getString("type"));
        Assert.assertEquals("some", json.getString("key"));
    }

}
