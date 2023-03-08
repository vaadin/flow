package com.vaadin.base.devserver.themeeditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.base.devserver.DebugWindowMessage;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
import com.vaadin.base.devserver.themeeditor.messages.ComponentMetadataResponse;
import com.vaadin.base.devserver.themeeditor.messages.ErrorResponse;
import com.vaadin.base.devserver.themeeditor.messages.LoadRulesResponse;
import com.vaadin.base.devserver.themeeditor.utils.CssRule;
import com.vaadin.flow.testutil.TestUtils;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.vaadin.base.devserver.themeeditor.ThemeEditorCommand.CODE_ERROR;
import static com.vaadin.base.devserver.themeeditor.ThemeEditorCommand.CODE_OK;

public class ThemeEditorMessageHandlerTest extends AbstractThemeEditorTest {

    @Before
    public void prepareFiles() throws IOException {
        super.prepare();
        copy("TestView_clean.java", "TestView.java");
        File themeFolder = TestUtils
                .getTestFolder(FRONTEND_FOLDER + "/themes/my-theme");
        File stylesCss = new File(themeFolder, "styles.css");
        if (stylesCss.exists()) {
            stylesCss.delete();
        }
        stylesCss.createNewFile();
        File themeEditorCss = new File(themeFolder, "theme-editor.css");
        if (themeEditorCss.exists()) {
            themeEditorCss.delete();
        }

    }

    @After
    public void cleanup() {
        File themeFolder = new File(
                TestUtils.getTestFolder(FRONTEND_NO_THEME_FOLDER), "themes");
        if (themeFolder.exists()) {
            new File(themeFolder, "my-theme/styles.css").delete();
            new File(themeFolder, "my-theme/theme-editor.css").delete();
            new File(themeFolder, "my-theme").delete();
            themeFolder.delete();
        }
        File javaFolder = TestUtils.getTestFolder("java/org/vaadin/example");
        File testView = new File(javaFolder, "TestView.java");
        if (testView.exists()) {
            testView.delete();
        }
    }

    @Test
    public void testCanHandle() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();

        JsonObject data = Json.createObject();
        data.put("requestId", "123");
        data.put("uiId", 0);
        Assert.assertTrue(handler.canHandle(ThemeEditorCommand.RULES, data));
        Assert.assertFalse(handler.canHandle("Random command", data));
        Assert.assertFalse(handler.canHandle(null, data));

        data = Json.createObject();
        Assert.assertFalse(handler.canHandle(ThemeEditorCommand.RULES, data));

        Assert.assertFalse(handler.canHandle(ThemeEditorCommand.RULES, null));
    }

    @Test
    public void testHandleThemeEditorRules() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        setRule(0, "id1", handler, "vaadin-button::part(label)", "color",
                "red");
    }

    @Test
    public void testHandleThemeEditorClassNames() {
        prepareComponentTracker(22);
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        setClassNames(0, "id1", handler, new String[] { "bold", "beautiful" },
                new String[] { "ugly" });
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
                ThemeEditorCommand.COMPONENT_METADATA, data);
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
        data.put("uiId", 0);

        BaseResponse response = handler
                .handleDebugMessageData(ThemeEditorCommand.LOAD_PREVIEW, data);
        assertResponseOk(response, requestId);
    }

    @Test
    public void testHandleLoadRules() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        loadRules(0, "id1", handler, "vaadin-button");
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
                .handleDebugMessageData(ThemeEditorCommand.CLASS_NAMES, data);
        assertResponseError(response, requestId);
    }

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String requestId = "abc-123";
        String message = "Cannot pick component";
        ErrorResponse errorResponse = new ErrorResponse(requestId, message);
        Object debugMessage = new DebugWindowMessage(
                ThemeEditorCommand.RESPONSE, errorResponse);
        String json = objectMapper.writeValueAsString(debugMessage);
        String expectedJson = String.format(
                "{\"command\":\"%s\",\"data\":{\"requestId\":\"%s\",\"message\":\"%s\",\"code\":\"error\"}}",
                ThemeEditorCommand.RESPONSE, requestId, message);
        Assert.assertEquals(expectedJson, json);
    }

    @Test
    public void testJsonNullSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String requestId = "abc-123";
        ErrorResponse errorResponse = new ErrorResponse(requestId, null);
        Object debugMessage = new DebugWindowMessage(
                ThemeEditorCommand.RESPONSE, errorResponse);
        String json = objectMapper.writeValueAsString(debugMessage);
        String expectedJson = String.format(
                "{\"command\":\"%s\",\"data\":{\"requestId\":\"%s\",\"code\":\"error\"}}",
                ThemeEditorCommand.RESPONSE, requestId);
        Assert.assertEquals(expectedJson, json);
    }

    @Test
    public void testHistoryUndo_ruleRevert() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set rules
        setRule(0, "id1", handler, "vaadin-button", "color", "red");
        setRule(0, "id2", handler, "vaadin-button", "color", "brown");
        // undo
        undoRedo(0, "id3", true, "id2", handler);
        // validate if value has been unset
        List<CssRule> rules = loadRules(0, "id4", handler, "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertEquals("red", rules.get(0).getProperties().get("color"));
    }

    @Test
    public void testHistoryUndo_ruleRemoved() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set rules
        setRule(0, "id1", handler, "vaadin-button", "color", "red");
        setRule(0, "id2", handler, "vaadin-button", "font-size", "12px");
        // undo
        undoRedo(0, "id3", true, "id2", handler);
        // validate if value has been unset
        List<CssRule> rules = loadRules(0, "id4", handler, "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertNull(rules.get(0).getProperties().get("font-size"));
    }

    @Test
    public void testHistoryRedo() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set rules
        setRule(0, "id1", handler, "vaadin-button", "color", "red");
        setRule(0, "id2", handler, "vaadin-button", "font-size", "12px");
        // undo
        undoRedo(0, "id3", true, "id2", handler);
        // validate if value has been unset
        List<CssRule> rules = loadRules(0, "id4", handler, "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertNull(rules.get(0).getProperties().get("font-size"));
        // redo
        undoRedo(0, "id4", false, "id2", handler);
        // validate if value has been set again
        rules = loadRules(0, "id4", handler, "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertEquals("12px",
                rules.get(0).getProperties().get("font-size"));
    }

    @Test
    public void testHistoryUndo_attributeRemoved() {
        prepareComponentTracker(22);
        JavaSourceModifier javaSourceModifierMock = Mockito
                .mock(JavaSourceModifier.class);
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler() {
            @Override
            public JavaSourceModifier getSourceModifier() {
                return javaSourceModifierMock;
            }
        };

        setClassNames(0, "id1", handler, new String[] { "bold", "beautiful" },
                new String[] {});
        Mockito.verify(javaSourceModifierMock, Mockito.times(1))
                .setClassNames(Mockito.any(), Mockito.any(), Mockito.any());

        // undo
        undoRedo(0, "id2", true, "id1", handler);
        Mockito.verify(javaSourceModifierMock, Mockito.times(1))
                .removeClassNames(Mockito.any(), Mockito.any(), Mockito.any());

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

    private List<CssRule> loadRules(Integer uiId, String requestId,
            ThemeEditorMessageHandler handler, String selector) {
        JsonObject request = Json.createObject();
        request.put("requestId", requestId);
        request.put("uiId", uiId);
        request.put("selectorFilter", selector);
        BaseResponse response = handler
                .handleDebugMessageData(ThemeEditorCommand.LOAD_RULES, request);
        assertResponseOk(response, requestId);
        Assert.assertTrue(response instanceof LoadRulesResponse);
        return ((LoadRulesResponse) response).getRules();
    }

    private void setRule(Integer uiId, String requestId,
            ThemeEditorMessageHandler handler, String selector, String property,
            String value) {
        JsonObject data = Json.createObject();
        data.put("requestId", requestId);
        data.put("uiId", uiId);

        JsonArray rules = Json.createArray();
        JsonObject rule = Json.createObject();
        rule.put("selector", selector);
        JsonObject properties = Json.createObject();
        properties.put(property, value);
        rule.put("properties", properties);
        rules.set(0, rule);
        data.put("rules", rules);
        BaseResponse response = handler
                .handleDebugMessageData(ThemeEditorCommand.RULES, data);
        assertResponseOk(response, requestId);
    }

    private void setClassNames(Integer uiId, String requestId,
            ThemeEditorMessageHandler handler, String[] toAdd,
            String[] toRemove) {
        JsonObject data = Json.createObject();
        data.put("uiId", uiId);
        data.put("nodeId", 0);
        data.put("requestId", requestId);
        JsonArray toBeAdded = Json.createArray();
        for (int i = 0; i < toAdd.length; ++i) {
            toBeAdded.set(i, toAdd[i]);
        }
        JsonArray toBeRemoved = Json.createArray();
        for (int i = 0; i < toRemove.length; ++i) {
            toBeRemoved.set(i, toRemove[i]);
        }
        data.put("add", toBeAdded);
        data.put("remove", toBeRemoved);
        BaseResponse response = handler
                .handleDebugMessageData(ThemeEditorCommand.CLASS_NAMES, data);
        assertResponseOk(response, requestId);
    }

    private void undoRedo(Integer uiId, String requestId, boolean undo,
            String undoId, ThemeEditorMessageHandler handler) {
        JsonObject data = Json.createObject();
        data.put("requestId", requestId);
        data.put("uiId", uiId);
        data.put(undo ? "undo" : "redo", undoId);
        BaseResponse response = handler
                .handleDebugMessageData(ThemeEditorCommand.HISTORY, data);
        assertResponseOk(response, requestId);
    }

}
