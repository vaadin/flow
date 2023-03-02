package com.vaadin.base.devserver.themeeditor;

import com.vaadin.base.devserver.themeeditor.messages.ClassNamesRequest;
import com.vaadin.base.devserver.themeeditor.messages.ComponentMetadataRequest;
import com.vaadin.base.devserver.themeeditor.messages.RulesRequest;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

public class ThemeEditorMessageHandlerTest extends AbstractThemeEditorTest {

    @Before
    public void prepare() {
        super.prepare();
        copy("TestView_clean.java", "TestView.java");
        prepareComponentTracker(22);
    }

    @Test
    public void testCanHandle() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();

        JsonObject data = Json.createObject();
        data.put("requestId", "123");
        Assert.assertTrue(handler.canHandle(RulesRequest.COMMAND_NAME, data));
        Assert.assertFalse(handler.canHandle("Random command", data));
        Assert.assertFalse(handler.canHandle(null, data));

        data = Json.createObject();
        Assert.assertFalse(handler.canHandle(RulesRequest.COMMAND_NAME, data));

        Assert.assertFalse(handler.canHandle(RulesRequest.COMMAND_NAME, null));
    }

    @Test
    public void testHandleThemeEditorRules() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        JsonObject data = Json.createObject();
        String requestId = UUID.randomUUID().toString();
        data.put("requestId", requestId);

        JsonArray toBeAdded = Json.createArray();
        JsonObject rule = Json.createObject();
        rule.put("selector", "vaadin-button::part(label)");
        rule.put("property", "color");
        rule.put("value", "red");
        toBeAdded.set(0, rule);
        toBeAdded.set(1, rule);

        JsonArray toBeRemoved = Json.createArray();
        toBeRemoved.set(0, rule);

        data.put("add", toBeAdded);
        data.put("remove", toBeRemoved);

        JsonObject response = handler
                .handleDebugMessageData(RulesRequest.COMMAND_NAME, data);
        Assert.assertEquals(requestId, response.getString("requestId"));
    }

    @Test
    public void testHandleThemeEditorClassNames() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        JsonObject data = Json.createObject();
        String requestId = UUID.randomUUID().toString();
        data.put("uiId", 0);
        data.put("nodeId", 0);
        data.put("requestId", requestId);
        JsonArray toBeAdded = Json.createArray();
        toBeAdded.set(0, "bold");
        toBeAdded.set(1, "beautiful");
        JsonArray toBeRemoved = Json.createArray();
        toBeRemoved.set(0, "ugly");
        data.put("add", toBeAdded);
        data.put("remove", toBeRemoved);
        JsonObject response = handler
                .handleDebugMessageData(ClassNamesRequest.COMMAND_NAME, data);
        Assert.assertEquals(requestId, response.getString("requestId"));
    }

    @Test
    public void testHandleComponentMetadata() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        JsonObject data = Json.createObject();
        String requestId = UUID.randomUUID().toString();
        data.put("uiId", 0);
        data.put("nodeId", 0);
        data.put("requestId", requestId);
        JsonObject response = handler.handleDebugMessageData(
                ComponentMetadataRequest.COMMAND_NAME, data);
        Assert.assertEquals(requestId, response.getString("requestId"));
        Assert.assertTrue(response.hasKey("accessible"));
    }

}
