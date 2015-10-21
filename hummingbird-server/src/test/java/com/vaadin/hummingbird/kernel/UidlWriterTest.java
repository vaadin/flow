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
    public void testTemplateOverrideNode() {
        BoundElementTemplate template = TemplateBuilder.withTag("div").build();

        Element templateElement = Element.getElement(template,
                StateNode.create());
        element.appendChild(templateElement);
        // Flush setup changes
        encodeElementChanges();

        templateElement.setAttribute("foo", "bar");

        JsonArray changes = encodeElementChanges();

        Assert.assertEquals(3, changes.length());

        JsonObject putOverride = changes.getObject(0);
        Assert.assertEquals("putOverride", putOverride.getString("type"));

        JsonObject putOverrideTemplate = changes.getObject(1);
        Assert.assertEquals("put", putOverrideTemplate.getString("type"));
        Assert.assertEquals("OVERRIDE_TEMPLATE",
                putOverrideTemplate.getString("key"));
        Assert.assertEquals(template.getId(),
                (int) putOverrideTemplate.getNumber("value"));

        JsonObject putAttribute = changes.getObject(2);
        Assert.assertEquals("put", putAttribute.getString("type"));
        Assert.assertEquals("foo", putAttribute.getString("key"));
        Assert.assertEquals("bar", putAttribute.getString("value"));
    }

    @Test
    public void testAddRemoveListener() {
        DomEventListener listener = e -> {
        };
        element.addEventListener("bar", listener);
        element.removeEventListener("bar", listener);
        element.addEventListener("bar", listener);

        JsonArray json = encodeElementChanges();
        System.out.println(json);
    }

}
