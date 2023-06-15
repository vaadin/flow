package com.vaadin.base.devserver.themeeditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.base.devserver.DebugWindowMessage;
import com.vaadin.base.devserver.OpenInCurrentIde;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
import com.vaadin.base.devserver.themeeditor.messages.ComponentMetadataResponse;
import com.vaadin.base.devserver.themeeditor.messages.ErrorResponse;
import com.vaadin.base.devserver.themeeditor.messages.LoadPreviewResponse;
import com.vaadin.base.devserver.themeeditor.messages.LoadRulesResponse;
import com.vaadin.base.devserver.themeeditor.utils.CssRule;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.testutil.TestUtils;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
    public void testHandle_Ok() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();

        JsonObject data = Json.createObject();
        data.put("requestId", "id1");
        data.put("uiId", 0);
        Assert.assertTrue(handler.canHandle(ThemeEditorCommand.RULES, data));
        Assert.assertFalse(handler.canHandle("Random command", data));
        Assert.assertFalse(handler.canHandle(null, data));

        data = Json.createObject();
        Assert.assertFalse(handler.canHandle(ThemeEditorCommand.RULES, data));

        Assert.assertFalse(handler.canHandle(ThemeEditorCommand.RULES, null));
    }

    @Test
    public void testHandle_Error() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();

        JsonObject data = Json.createObject();
        data.put("requestId", "id1");
        data.put("uiId", 0);

        BaseResponse response = handler.handleDebugMessageData(
                ThemeEditorCommand.COMPONENT_METADATA, data);
        assertResponseError(response, "id1");
    }

    @Test
    public void testHandle_ComponentMetadata() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        JsonObject data = Json.createObject();
        data.put("requestId", "id1");
        data.put("uiId", 0);
        data.put("nodeId", 0);
        BaseResponse response = handler.handleDebugMessageData(
                ThemeEditorCommand.COMPONENT_METADATA, data);
        assertResponseOk(response, "id1");

        Assert.assertTrue(response instanceof ComponentMetadataResponse);
        ComponentMetadataResponse metadataResponse = (ComponentMetadataResponse) response;
        Assert.assertTrue(metadataResponse.isAccessible());
        Assert.assertNull(metadataResponse.getClassName());
        Assert.assertNotNull(metadataResponse.getSuggestedClassName());
        Assert.assertEquals("test-view-span-1",
                metadataResponse.getSuggestedClassName());
    }

    @Test
    public void testHandle_LocalClassName() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        setLocalClassName(0, "id1", handler, "nice-button");
        JsonObject data = Json.createObject();
        data.put("requestId", "id2");
        data.put("uiId", 0);
        data.put("nodeId", 0);
        BaseResponse response = handler.handleDebugMessageData(
                ThemeEditorCommand.COMPONENT_METADATA, data);
        assertResponseOk(response, "id2");
        Assert.assertTrue(response instanceof ComponentMetadataResponse);
        ComponentMetadataResponse metadataResponse = (ComponentMetadataResponse) response;
        Assert.assertEquals("nice-button", metadataResponse.getClassName());
    }

    @Test
    public void testHandle_Rules_LoadPreview() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        JsonObject data = Json.createObject();
        data.put("requestId", "id3");
        data.put("uiId", 0);

        // set rules
        BaseResponse response = setRule(0, "id1", handler, SELECTOR_WITH_PART,
                "color", "red");
        assertResponseOk(response, "id1");
        response = setRule(0, "id2", handler, SELECTOR_WITH_PART, "font-size",
                "12px");
        assertResponseOk(response, "id2");

        // load preview
        response = handler
                .handleDebugMessageData(ThemeEditorCommand.LOAD_PREVIEW, data);
        assertResponseOk(response, "id3");

        Assert.assertTrue(response instanceof LoadPreviewResponse);
        String expected = "vaadin-text-field::part(label){color:red;font-size:12px}";
        LoadPreviewResponse loadPreviewResponse = (LoadPreviewResponse) response;
        Assert.assertEquals(expected, loadPreviewResponse.getCss());
    }

    @Test
    public void testHandle_LoadRules() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);

        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();

        // set rule
        BaseResponse response = setRule(0, "id1", handler, SELECTOR_WITH_PART,
                "color", "red");
        assertResponseOk(response, "id1");

        // override rule
        response = setRule(0, "id2", handler, SELECTOR_WITH_PART, "color",
                "brown");
        assertResponseOk(response, "id2");

        // assert single rule with single property
        List<CssRule> rules = loadRules(0, "id3", handler, SELECTOR_WITH_PART);
        Assert.assertEquals(1, rules.size());
        Assert.assertEquals(1, rules.get(0).getProperties().size());
        Assert.assertEquals("brown", rules.get(0).getProperties().get("color"));

        // load non existing rules
        rules = loadRules(0, "id4", handler, "vaadin-text-field");
        Assert.assertEquals(0, rules.size());
    }

    @Test
    public void testHandle_HistoryUndo_ruleRevert() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set rules
        BaseResponse response = setRule(0, "id1", handler, "vaadin-button",
                "color", "red");
        assertResponseOk(response, "id1");
        response = setRule(0, "id2", handler, "vaadin-button", "color",
                "brown");
        assertResponseOk(response, "id2");
        // undo
        undoRedo(0, "id3", true, "id2", handler);
        // validate if value has been unset
        List<CssRule> rules = loadRules(0, "id4", handler, "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertEquals("red", rules.get(0).getProperties().get("color"));
    }

    @Test
    public void testHandle_HistoryUndo_ruleRemoved() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set rules
        BaseResponse response = setRule(0, "id1", handler, "vaadin-button",
                "color", "red");
        assertResponseOk(response, "id1");
        response = setRule(0, "id2", handler, "vaadin-button", "font-size",
                "12px");
        assertResponseOk(response, "id2");
        // undo
        undoRedo(0, "id3", true, "id2", handler);
        // validate if value has been unset
        List<CssRule> rules = loadRules(0, "id4", handler, "vaadin-button");
        Assert.assertEquals(1, rules.size());
        Assert.assertNull(rules.get(0).getProperties().get("font-size"));
    }

    @Test
    public void testHandle_HistoryRedo() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set rules
        BaseResponse response = setRule(0, "id1", handler, "vaadin-button",
                "color", "red");
        assertResponseOk(response, "id1");
        response = setRule(0, "id2", handler, "vaadin-button", "font-size",
                "12px");
        assertResponseOk(response, "id2");

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
    public void testHandle_HistoryUndo_localClassName_removed() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        JavaSourceModifier javaSourceModifierMock = Mockito
                .mock(JavaSourceModifier.class);
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler() {
            @Override
            public JavaSourceModifier getSourceModifier() {
                return javaSourceModifierMock;
            }
        };

        setLocalClassName(0, "id1", handler, "bold-style");
        Mockito.verify(javaSourceModifierMock, Mockito.times(1))
                .setLocalClassName(Mockito.same(0), Mockito.same(0),
                        Mockito.anyString());

        // undo
        undoRedo(0, "id2", true, "id1", handler);
        Mockito.verify(javaSourceModifierMock, Mockito.times(1))
                .removeLocalClassName(Mockito.same(0), Mockito.same(0));

    }

    @Test
    public void testHandle_MarkAsUsed() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        JsonObject data = Json.createObject();
        data.put("requestId", "id1");
        data.put("uiId", 0);

        try (MockedStatic<UsageStatistics> usageStatistics = Mockito
                .mockStatic(UsageStatistics.class)) {
            MockedStatic.Verification verification = () -> UsageStatistics
                    .markAsUsed(Mockito.eq("flow/ThemeEditor"), Mockito.any());
            handler.handleDebugMessageData(ThemeEditorCommand.MARK_AS_USED,
                    data);
            usageStatistics.verify(verification);
        }
    }

    @Test
    public void testHandle_OpenCss() {
        ThemeEditorMessageHandler handler = new TestThemeEditorMessageHandler();
        // set rules
        BaseResponse response = setRule(0, "id1", handler, "vaadin-button",
                "color", "red");
        assertResponseOk(response, "id1");

        // request existing selector
        // expected: 4 lines of comment + 1 = 5
        JsonObject data = Json.createObject();
        data.put("requestId", "id2");
        data.put("uiId", 0);
        data.put("selector", "vaadin-button");
        try (MockedStatic<OpenInCurrentIde> openInIde = Mockito
                .mockStatic(OpenInCurrentIde.class)) {
            MockedStatic.Verification verification = () -> OpenInCurrentIde
                    .openFile(Mockito.any(), Mockito.eq(5));
            openInIde.when(verification).thenReturn(true);
            response = handler
                    .handleDebugMessageData(ThemeEditorCommand.OPEN_CSS, data);
            openInIde.verify(verification);
            assertResponseOk(response, "id2");
        }

        // request non-existing selector
        // expected: 4 lines of comment + 2 lines of vaadin-button with empty
        // line + 1 = 7
        data.put("requestId", "id3");
        data.put("selector", "vaadin-text-field");
        try (MockedStatic<OpenInCurrentIde> openInIde = Mockito
                .mockStatic(OpenInCurrentIde.class)) {
            MockedStatic.Verification verification = () -> OpenInCurrentIde
                    .openFile(Mockito.any(), Mockito.eq(7));
            openInIde.when(verification).thenReturn(true);
            response = handler
                    .handleDebugMessageData(ThemeEditorCommand.OPEN_CSS, data);
            openInIde.verify(verification);
            assertResponseOk(response, "id3");
        }

        // request non-existing selector, CSS is sorted - should be on top
        // expected: 4 lines of comment + 1 = 5
        data.put("requestId", "id4");
        data.put("selector", "vaadin-app-layout");
        try (MockedStatic<OpenInCurrentIde> openInIde = Mockito
                .mockStatic(OpenInCurrentIde.class)) {
            MockedStatic.Verification verification = () -> OpenInCurrentIde
                    .openFile(Mockito.any(), Mockito.eq(5));
            openInIde.when(verification).thenReturn(true);
            response = handler
                    .handleDebugMessageData(ThemeEditorCommand.OPEN_CSS, data);
            openInIde.verify(verification);
            assertResponseOk(response, "id4");
        }

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
        request.put("selectors",
                JsonUtils.listToJson(Collections.singletonList(selector)));
        BaseResponse response = handler
                .handleDebugMessageData(ThemeEditorCommand.LOAD_RULES, request);
        assertResponseOk(response, requestId);
        Assert.assertTrue(response instanceof LoadRulesResponse);
        return ((LoadRulesResponse) response).getRules();
    }

    private BaseResponse setRule(Integer uiId, String requestId,
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
        return handler.handleDebugMessageData(ThemeEditorCommand.RULES, data);
    }

    private void setLocalClassName(Integer uiId, String requestId,
            ThemeEditorMessageHandler handler, String localClassName) {
        JsonObject data = Json.createObject();
        data.put("uiId", uiId);
        data.put("nodeId", 0);
        data.put("requestId", requestId);
        data.put("className", localClassName);
        BaseResponse response = handler.handleDebugMessageData(
                ThemeEditorCommand.LOCAL_CLASS_NAME, data);
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
