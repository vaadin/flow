package com.vaadin.base.devserver.themeeditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.base.devserver.DebugWindowMessage;
import com.vaadin.base.devserver.themeeditor.messages.*;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static com.vaadin.base.devserver.themeeditor.messages.BaseResponse.CODE_ERROR;
import static com.vaadin.base.devserver.themeeditor.messages.BaseResponse.CODE_OK;

public class ThemeEditorMessageHandlerTest extends AbstractThemeEditorTest {

    @Before
    public void prepare() {
        super.prepare();
        copy("TestView_clean.java", "TestView.java");
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

        BaseResponse response = handler
                .handleDebugMessageData(RulesRequest.COMMAND_NAME, data);
        assertResponseOk(response, requestId);
    }

    @Test
    public void testHandleThemeEditorClassNames()
            throws JsonProcessingException {
        prepareComponentTracker(22);
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
        BaseResponse response = handler
                .handleDebugMessageData(ClassNamesRequest.COMMAND_NAME, data);
        assertResponseOk(response, requestId);
    }

    @Test
    public void testHandleComponentMetadata() {
        prepareComponentTracker(22);
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        JsonObject data = Json.createObject();
        String requestId = UUID.randomUUID().toString();
        data.put("uiId", 0);
        data.put("nodeId", 0);
        data.put("requestId", requestId);
        BaseResponse response = handler.handleDebugMessageData(
                ComponentMetadataRequest.COMMAND_NAME, data);
        assertResponseOk(response, requestId);
        Assert.assertTrue(
                ((ComponentMetadataResponse) response).isAccessible());
    }

    @Test
    public void testHandleLoadPreview() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        JsonObject data = Json.createObject();
        String requestId = UUID.randomUUID().toString();
        data.put("requestId", requestId);

        BaseResponse response = handler.handleDebugMessageData(
                LoadPreviewRequest.COMMAND_NAME, data);
        assertResponseOk(response, requestId);
    }

    @Test
    public void testHandleLoadRules() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        JsonObject data = Json.createObject();
        String requestId = UUID.randomUUID().toString();
        data.put("requestId", requestId);
        data.put("selectorFilter", "vaadin-button");

        BaseResponse response = handler.handleDebugMessageData(
                LoadRulesRequest.COMMAND_NAME, data);
        assertResponseOk(response, requestId);
    }

    @Test
    public void testError() {
        prepareComponentTracker(42);
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
        BaseResponse response = handler
                .handleDebugMessageData(ClassNamesRequest.COMMAND_NAME, data);
        assertResponseError(response, requestId);
    }

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String requestId = "abc-123";
        String message = "Cannot pick component";
        ErrorResponse errorResponse = new ErrorResponse(requestId, message);
        Object debugMessage = new DebugWindowMessage(BaseResponse.COMMAND_NAME,
                errorResponse);
        String json = objectMapper.writeValueAsString(debugMessage);
        String expectedJson = String.format(
                "{\"command\":\"%s\",\"data\":{\"requestId\":\"%s\",\"code\":\"error\",\"message\":\"%s\"}}",
                BaseResponse.COMMAND_NAME, requestId, message);
        Assert.assertEquals(expectedJson, json);
    }

    @Test
    public void testJsonNullSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String requestId = "abc-123";
        ErrorResponse errorResponse = new ErrorResponse(requestId, null);
        Object debugMessage = new DebugWindowMessage(BaseResponse.COMMAND_NAME,
                errorResponse);
        String json = objectMapper.writeValueAsString(debugMessage);
        String expectedJson = String.format(
                "{\"command\":\"%s\",\"data\":{\"requestId\":\"%s\",\"code\":\"error\"}}",
                BaseResponse.COMMAND_NAME, requestId);
        Assert.assertEquals(expectedJson, json);
    }

    private void assertResponseOk(BaseResponse response, String requestId) {
        Assert.assertEquals(requestId, response.getRequestId());
        Assert.assertEquals(CODE_OK, response.getCode());
    }

    private void assertResponseError(BaseResponse response, String requestId) {
        Assert.assertTrue(response instanceof ErrorResponse);
        Assert.assertEquals(requestId, response.getRequestId());
        Assert.assertEquals(CODE_ERROR, response.getCode());
        Assert.assertNotNull(((ErrorResponse) response).getMessage());
    }

}
