package com.vaadin.base.devserver.themeeditor;

import com.vaadin.base.devserver.themeeditor.messages.*;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.VaadinContext;
import elemental.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Handler for ThemeEditor debug window communication messages. Responsible for
 * preparing data for {@link ThemeModifier} and {@link JavaSourceModifier}.
 */
public class ThemeEditorMessageHandler {

    private final JavaSourceModifier sourceModifier;

    private final ThemeModifier themeModifier;

    private final Map<String, Function<JsonObject, BaseResponse>> handlers;

    public ThemeEditorMessageHandler(VaadinContext context) {
        this.sourceModifier = new JavaSourceModifier(context);
        this.themeModifier = new ThemeModifier(context);
        this.handlers = Map.of(RulesRequest.COMMAND_NAME,
                this::handleThemeEditorRules, ClassNamesRequest.COMMAND_NAME,
                this::handleThemeEditorClassNames,
                ComponentMetadataRequest.COMMAND_NAME,
                this::handleComponentMetadata, LoadPreviewRequest.COMMAND_NAME,
                this::handleLoadPreview, LoadRulesRequest.COMMAND_NAME,
                this::handleLoadRules);
    }

    public boolean isEnabled() {
        return themeModifier.isEnabled() && sourceModifier.isEnabled();
    }

    public String getState() {
        return themeModifier.getState().name().toLowerCase();
    }

    protected JavaSourceModifier getSourceModifier() {
        return sourceModifier;
    }

    protected ThemeModifier getThemeModifier() {
        return themeModifier;
    }

    /**
     * Checks if given command can be handled by ThemeEditor.
     *
     * @param command
     *            command to be verified if supported
     * @param data
     *            data object to be verified if is of proper structure
     * @return true if it can be handled, false otherwise
     */
    public boolean canHandle(String command, JsonObject data) {
        return command != null && data != null && data.hasKey("requestId")
                && handlers.containsKey(command);
    }

    /**
     * Handles debug message command and performs given action.
     *
     * @param command
     *            Command name
     * @param data
     *            Command data
     * @return response in form of JsonObject
     */
    public BaseResponse handleDebugMessageData(String command,
            JsonObject data) {
        assert canHandle(command, data);
        try {
            return handlers.get(command).apply(data);
        } catch (ThemeEditorException ex) {
            getLogger().error(ex.getMessage(), ex);
            return new ErrorResponse(data.getString("requestId"),
                    ex.getMessage());
        }
    }

    protected BaseResponse handleThemeEditorRules(JsonObject data) {
        RulesRequest request = JsonUtils.readToObject(data, RulesRequest.class);
        if (request.getAdd() != null) {
            getThemeModifier().setThemeProperties(request.getAdd());
        }
        if (request.getRemove() != null) {
            getThemeModifier().removeThemeProperties(request.getRemove());
        }
        return BaseResponse.ok(request.getRequestId());
    }

    protected BaseResponse handleThemeEditorClassNames(JsonObject data) {
        ClassNamesRequest request = JsonUtils.readToObject(data,
                ClassNamesRequest.class);
        int uiId = request.getUiId();
        int nodeId = request.getNodeId();
        if (request.getAdd() != null) {
            getSourceModifier().setClassNames(uiId, nodeId, request.getAdd());
        }
        if (request.getRemove() != null) {
            getSourceModifier().removeClassNames(uiId, nodeId,
                    request.getRemove());
        }
        return BaseResponse.ok(request.getRequestId());
    }

    protected BaseResponse handleComponentMetadata(JsonObject data) {
        ComponentMetadataRequest request = JsonUtils.readToObject(data,
                ComponentMetadataRequest.class);
        JavaSourceModifier.ComponentMetadata metadata = getSourceModifier()
                .getMetadata(request.getUiId(), request.getNodeId());
        return new ComponentMetadataResponse(request.getRequestId(),
                metadata.isAccessible());
    }

    protected BaseResponse handleLoadPreview(JsonObject data) {
        LoadPreviewRequest request = JsonUtils.readToObject(data,
                LoadPreviewRequest.class);
        String css = getThemeModifier().getCss();
        return new LoadPreviewResponse(request.getRequestId(), css);
    }

    protected BaseResponse handleLoadRules(JsonObject data) {
        LoadRulesRequest request = JsonUtils.readToObject(data,
                LoadRulesRequest.class);
        List<LoadRulesResponse.CssRule> rules = getThemeModifier()
                .getCssRules(request.getSelectorFilter());
        return new LoadRulesResponse(request.getRequestId(), rules);
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(ThemeEditorMessageHandler.class.getName());
    }

}
