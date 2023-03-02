package com.vaadin.base.devserver.themeeditor;

import com.vaadin.base.devserver.themeeditor.messages.*;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.VaadinContext;
import elemental.json.JsonObject;

import java.util.Map;
import java.util.function.Function;

/**
 * Handler for ThemeEditor debug window communication messages. Responsible for
 * preparing data for {@link ThemeModifier} and {@link JavaSourceModifier}.
 */
public class ThemeEditorMessageHandler {

    private final JavaSourceModifier sourceModifier;

    private final ThemeModifier themeModifier;

    private final Map<String, Function<JsonObject, JsonObject>> handlers;

    public ThemeEditorMessageHandler(VaadinContext context) {
        this.sourceModifier = new JavaSourceModifier(context);
        this.themeModifier = new ThemeModifier(context);
        this.handlers = Map.of(RulesRequest.COMMAND_NAME,
                this::handleThemeEditorRules, ClassNamesRequest.COMMAND_NAME,
                this::handleThemeEditorClassNames,
                ComponentMetadataRequest.COMMAND_NAME,
                this::handleComponentMetadata);
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
    public JsonObject handleDebugMessageData(String command, JsonObject data) {
        assert canHandle(command, data);
        return handlers.get(command).apply(data);
    }

    protected JsonObject handleThemeEditorRules(JsonObject data) {
        RulesRequest request = JsonUtils.readToObject(data, RulesRequest.class);
        if (request.add() != null) {
            getThemeModifier().setThemeProperties(request.add());
        }
        if (request.remove() != null) {
            getThemeModifier().removeThemeProperties(request.remove());
        }
        return ok(request.requestId());
    }

    protected JsonObject handleThemeEditorClassNames(JsonObject data) {
        ClassNamesRequest request = JsonUtils.readToObject(data,
                ClassNamesRequest.class);
        int uiId = request.uiId();
        int nodeId = request.nodeId();
        if (request.add() != null) {
            getSourceModifier().setClassNames(uiId, nodeId, request.add());
        }
        if (request.remove() != null) {
            getSourceModifier().removeClassNames(uiId, nodeId,
                    request.remove());
        }
        return ok(request.requestId());
    }

    protected JsonObject handleComponentMetadata(JsonObject data) {
        ComponentMetadataRequest request = JsonUtils.readToObject(data,
                ComponentMetadataRequest.class);
        JavaSourceModifier.ComponentMetadata metadata = getSourceModifier()
                .getMetadata(request.uiId(), request.nodeId());
        ComponentMetadataResponse response = new ComponentMetadataResponse(
                request.requestId(), metadata.isAccessible());
        return JsonUtils.beanToJson(response);
    }

    private JsonObject ok(String requestId) {
        return JsonUtils.beanToJson(new GenericResponse(requestId));
    }

}
