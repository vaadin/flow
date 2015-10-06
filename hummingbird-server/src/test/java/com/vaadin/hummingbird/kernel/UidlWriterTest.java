package com.vaadin.hummingbird.kernel;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.communication.TransactionLogBuilder;
import com.vaadin.server.communication.TransactionLogJsonProducer;
import com.vaadin.server.communication.TransactionLogOptimizer;
import com.vaadin.ui.UI;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class UidlWriterTest {
    private static class UidlWriterTestUI extends UI {
        @Override
        protected void init(VaadinRequest request) {
            // Do nothing
        }
    }

    private Element element;
    private UI ui;

    @Before
    public void setup() {
        ui = new UI() {
            @Override
            protected void init(VaadinRequest request) {
                // Do nothing
            }
        };
        ui.getRoot().getRootNode().commit();

        element = ui.getElement();
    }

    private JsonArray encodeElementChanges() {
        TransactionLogBuilder logBuilder = new TransactionLogBuilder();
        ui.getRoot().getRootNode().commit(logBuilder.getVisitor());

        TransactionLogOptimizer optimizer = new TransactionLogOptimizer(ui,
                logBuilder.getChanges(), logBuilder.getTemplates());

        TransactionLogJsonProducer jsonProducer = new TransactionLogJsonProducer(
                ui, optimizer.getChanges(), optimizer.getTemplates());

        return jsonProducer.getChangesJson();
    }

    @Test
    public void testBooleanAttribute_booleanChangeValue() {
        element.setAttribute("foo", true);

        JsonArray changes = encodeElementChanges();

        Assert.assertEquals(1, changes.length());

        JsonObject change = changes.getObject(0);

        Assert.assertEquals("put", change.getString("type"));
        Assert.assertEquals("foo", change.getString("key"));

        JsonValue value = change.get("value");

        Assert.assertNotNull(value);
        Assert.assertEquals(JsonType.BOOLEAN, value.getType());
        Assert.assertTrue(value.asBoolean());
    }

    @Test
    public void removedElementHasTag() {
        Element child = new Element("div");
        element.appendChild(child);
        child.removeFromParent();

        JsonArray changes = encodeElementChanges();
        Assert.assertEquals(3, changes.length());

        JsonObject insert = changes.getObject(0);
        Assert.assertEquals("listInsertNode", insert.getString("type"));
        Assert.assertEquals("CHILDREN", insert.getString("key"));
        Assert.assertEquals(3, (int) insert.getNumber("id"));
        Assert.assertEquals(4, (int) insert.getNumber("value"));
        Assert.assertEquals(0, (int) insert.getNumber("index"));

        JsonObject remove = changes.getObject(1);
        Assert.assertEquals("listRemove", remove.getString("type"));
        Assert.assertEquals("CHILDREN", remove.getString("key"));
        Assert.assertEquals(3, (int) remove.getNumber("id"));
        Assert.assertEquals(0, (int) remove.getNumber("index"));

        JsonObject put = changes.getObject(2);
        Assert.assertEquals("put", put.getString("type"));
        Assert.assertEquals("TAG", put.getString("key"));
        Assert.assertEquals(4, (int) put.getNumber("id"));
        Assert.assertEquals("div", put.getString("value"));
    }
}
