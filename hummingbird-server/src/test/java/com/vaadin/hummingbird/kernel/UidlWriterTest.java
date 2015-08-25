package com.vaadin.hummingbird.kernel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.communication.ChangeUidlBuilder;
import com.vaadin.ui.UI;

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
        ui.getRootNode().commit();

        element = ui.getElement();
    }

    private JsonArray encodeElementChanges() {
        ChangeUidlBuilder visitor = new ChangeUidlBuilder(ui);
        ui.getRootNode().commit(visitor);
        return visitor.getChanges();
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
}
