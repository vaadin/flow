package com.vaadin.base.devserver.themeeditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.base.devserver.DebugWindowMessage;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
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

import static com.vaadin.base.devserver.themeeditor.JavaSourceModifier.UNIQUE_CLASSNAME_PREFIX;
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
        prepareComponentTracker(TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set global rules for vaadin-button
        BaseResponse response = setRule(0, null, "id1", handler,
                "vaadin-button", "label", "color", "red");
        assertResponseOk(response, "id1");
        // set instance specific rules for given vaadin-button
        response = setRule(0, 0, "id2", handler, "vaadin-button", "label",
                "color", "brown");
        assertResponseOk(response, "id2");
    }

    @Test
    public void testHandleThemeEditorClassNames() {
        prepareComponentTracker(TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        setClassNames(0, "id1", handler, new String[] { "bold", "beautiful" },
                new String[] { "ugly" });
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
        prepareComponentTracker(TEXTFIELD_CREATE, TEXTFIELD_ATTACH);

        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        BaseResponse response = setRule(0, null, "id1", handler,
                "vaadin-button", "label", "color", "red");
        assertResponseOk(response, "id1");
        response = setRule(0, 0, "id2", handler, "vaadin-button", "label",
                "color", "brown");
        assertResponseOk(response, "id2");

        List<CssRule> rules = loadRules(0, 0, "id3", handler, "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertNotNull(rules.get(0).getClassName());
        Assert.assertTrue(rules.get(0).getClassName()
                .startsWith(UNIQUE_CLASSNAME_PREFIX));
        Assert.assertEquals("brown", rules.get(0).getProperties().get("color"));

        rules = loadRules(0, null, "id4", handler, "vaadin-button");
        Assert.assertEquals(2, rules.size());

        rules = loadRules(0, null, "id5", handler, "vaadin-button-fake");
        Assert.assertEquals(0, rules.size());
    }

    @Test
    public void testHandleRules_notAccessible_exceptionIsThrown() {
        prepareComponentTracker(42, 42);
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set instance specific rules for given vaadin-button
        BaseResponse response = setRule(0, 0, "id1", handler, "vaadin-button",
                "label", "color", "brown");
        assertResponseError(response, "id1");
    }

    @Test
    public void testHandleRules_noUniqueRules() {
        prepareComponentTracker(TEXTFIELD_CREATE, TEXTFIELD_ATTACH);

        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        BaseResponse response = setRule(0, null, "id1", handler,
                "vaadin-button", "label", "color", "red");
        assertResponseOk(response, "id1");
        response = setRule(0, null, "id2", handler, "vaadin-button", "label",
                "font-size", "12px");
        assertResponseOk(response, "id2");

        List<CssRule> rules = loadRules(0, 0, "id3", handler, "vaadin-button",
                false);
        Assert.assertEquals(0, rules.size());
    }

    @Test
    public void testError() {
        prepareComponentTracker(42, 42);
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
        BaseResponse response = setRule(0, null, "id1", handler,
                "vaadin-button", null, "color", "red");
        assertResponseOk(response, "id1");
        response = setRule(0, null, "id2", handler, "vaadin-button", null,
                "color", "brown");
        assertResponseOk(response, "id2");
        // undo
        undoRedo(0, "id3", true, "id2", handler);
        // validate if value has been unset
        List<CssRule> rules = loadRules(0, null, "id4", handler,
                "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertEquals("red", rules.get(0).getProperties().get("color"));
    }

    @Test
    public void testHistoryUndo_ruleRemoved() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set rules
        BaseResponse response = setRule(0, null, "id1", handler,
                "vaadin-button", null, "color", "red");
        assertResponseOk(response, "id1");
        response = setRule(0, null, "id2", handler, "vaadin-button", null,
                "font-size", "12px");
        assertResponseOk(response, "id2");
        // undo
        undoRedo(0, "id3", true, "id2", handler);
        // validate if value has been unset
        List<CssRule> rules = loadRules(0, null, "id4", handler,
                "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertNull(rules.get(0).getProperties().get("font-size"));
    }

    @Test
    public void testHistoryRedo() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set rules
        BaseResponse response = setRule(0, null, "id1", handler,
                "vaadin-button", null, "color", "red");
        assertResponseOk(response, "id1");
        response = setRule(0, null, "id2", handler, "vaadin-button", null,
                "font-size", "12px");
        assertResponseOk(response, "id2");

        // undo
        undoRedo(0, "id3", true, "id2", handler);
        // validate if value has been unset
        List<CssRule> rules = loadRules(0, null, "id4", handler,
                "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertNull(rules.get(0).getProperties().get("font-size"));
        // redo
        undoRedo(0, "id4", false, "id2", handler);
        // validate if value has been set again
        rules = loadRules(0, null, "id4", handler, "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertEquals("12px",
                rules.get(0).getProperties().get("font-size"));
    }

    @Test
    public void testHistoryUndo_attributeRemoved() {
        prepareComponentTracker(TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
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

    private List<CssRule> loadRules(Integer uiId, Integer nodeId,
            String requestId, ThemeEditorMessageHandler handler,
            String selector) {
        return loadRules(uiId, nodeId, requestId, handler, selector, true);
    }

    private List<CssRule> loadRules(Integer uiId, Integer nodeId,
            String requestId, ThemeEditorMessageHandler handler,
            String selector, boolean hasUniqueClassName) {
        JsonObject request = Json.createObject();
        request.put("requestId", requestId);
        request.put("uiId", uiId);
        if (nodeId != null) {
            request.put("nodeId", nodeId);
        }
        request.put("selectorFilter", selector);
        BaseResponse response = handler
                .handleDebugMessageData(ThemeEditorCommand.LOAD_RULES, request);
        assertResponseOk(response, requestId);
        Assert.assertTrue(response instanceof LoadRulesResponse);
        if (nodeId != null) {
            Assert.assertEquals(Boolean.TRUE,
                    ((LoadRulesResponse) response).isAccessible());
            if (hasUniqueClassName) {
                Assert.assertNotNull(
                        ((LoadRulesResponse) response).getClassName());
            }
        }
        return ((LoadRulesResponse) response).getRules();
    }

    private BaseResponse setRule(Integer uiId, Integer nodeId, String requestId,
            ThemeEditorMessageHandler handler, String tagName, String partName,
            String property, String value) {
        JsonObject data = Json.createObject();
        data.put("requestId", requestId);
        data.put("uiId", uiId);
        if (nodeId != null) {
            data.put("nodeId", nodeId);
        }

        JsonArray rules = Json.createArray();
        JsonObject rule = Json.createObject();
        rule.put("tagName", tagName);
        if (partName != null) {
            rule.put("partName", partName);
        }
        JsonObject properties = Json.createObject();
        properties.put(property, value);
        rule.put("properties", properties);
        rules.set(0, rule);
        data.put("rules", rules);
        return handler.handleDebugMessageData(ThemeEditorCommand.RULES, data);
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
