package com.vaadin.hummingbird.change;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.StateNodeTest;
import com.vaadin.hummingbird.namespace.AbstractNamespaceTest;
import com.vaadin.hummingbird.namespace.ElementPropertiesNamespace;
import com.vaadin.hummingbird.namespace.MapNamespace;
import com.vaadin.hummingbird.namespace.NamespaceRegistry;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class MapPutChangeTest {
    private MapNamespace namespace = AbstractNamespaceTest
            .createNamespace(ElementPropertiesNamespace.class);

    @Test
    public void testJson() {
        MapPutChange change = new MapPutChange(namespace, "some", "string");

        JsonObject json = change.toJson();

        Assert.assertEquals(change.getNode().getId(),
                (int) json.getNumber("node"));
        Assert.assertEquals(NamespaceRegistry.getId(namespace.getClass()),
                (int) json.getNumber("ns"));
        Assert.assertEquals("put", json.getString("type"));
        Assert.assertEquals("some", json.getString("key"));
        Assert.assertEquals("string", json.getString("value"));
    }

    @Test
    public void testJsonValueTypes() {
        JsonValue stringValue = getValue("string");
        Assert.assertSame(JsonType.STRING, stringValue.getType());
        Assert.assertEquals("string", stringValue.asString());

        JsonValue numberValue = getValue(Integer.valueOf(1));
        Assert.assertSame(JsonType.NUMBER, numberValue.getType());
        Assert.assertEquals(1, numberValue.asNumber(), 0);

        JsonValue booleanValue = getValue(Boolean.TRUE);
        Assert.assertSame(JsonType.BOOLEAN, booleanValue.getType());
        Assert.assertTrue(booleanValue.asBoolean());

        JsonObject jsonInput = Json.createObject();
        JsonValue jsonValue = getValue(jsonInput);
        Assert.assertSame(JsonType.OBJECT, jsonValue.getType());
        Assert.assertSame(jsonInput, jsonValue);
    }

    @Test
    public void testNodeValueType() {
        StateNode value = StateNodeTest.createEmptyNode("value");
        MapPutChange change = new MapPutChange(namespace, "key", value);

        JsonObject json = change.toJson();
        Assert.assertFalse(json.hasKey("value"));

        JsonValue nodeValue = json.get("nodeValue");
        Assert.assertSame(JsonType.NUMBER, nodeValue.getType());
        Assert.assertEquals(value.getId(), (int) nodeValue.asNumber());
    }

    private JsonValue getValue(Object input) {
        MapPutChange change = new MapPutChange(namespace, "key", input);
        JsonObject json = change.toJson();
        return json.get("value");
    }

}
