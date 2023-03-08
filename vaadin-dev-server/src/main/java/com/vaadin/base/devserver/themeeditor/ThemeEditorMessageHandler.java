package com.vaadin.base.devserver.themeeditor;

import com.vaadin.base.devserver.themeeditor.handlers.*;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
import com.vaadin.base.devserver.themeeditor.messages.ErrorResponse;
import com.vaadin.base.devserver.themeeditor.utils.*;
import com.vaadin.flow.server.VaadinContext;
import elemental.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Handler for ThemeEditor debug window communication messages. Responsible for
 * preparing data for {@link ThemeModifier} and {@link JavaSourceModifier}.
 */
public class ThemeEditorMessageHandler
        implements HasSourceModifier, HasThemeModifier {

    private final JavaSourceModifier sourceModifier;

    private final ThemeModifier themeModifier;

    private final Set<MessageHandler> handlers = new HashSet<>();

    public ThemeEditorMessageHandler(VaadinContext context) {
        this.sourceModifier = new JavaSourceModifier(context);
        this.themeModifier = new ThemeModifier(context);
        this.handlers.add(new RulesHandler(this));
        this.handlers.add(new ClassNamesHandler(this));
        this.handlers.add(new ComponentMetadataHandler(this));
        this.handlers.add(new HistoryHandler());
        this.handlers.add(new LoadRulesHandler(this));
        this.handlers.add(new LoadPreviewHandler(this));
    }

    public boolean isEnabled() {
        return getThemeModifier().isEnabled()
                && getSourceModifier().isEnabled();
    }

    public String getState() {
        return getThemeModifier().getState().name().toLowerCase();
    }

    @Override
    public JavaSourceModifier getSourceModifier() {
        return sourceModifier;
    }

    @Override
    public ThemeModifier getThemeModifier() {
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
                && getHandler(command).isPresent();
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
        String requestId = data.getString("requestId");
        Integer uiId = (int) data.getNumber("uiId");
        ThemeEditorHistory history = ThemeEditorHistory.forUi(uiId);
        try {
            MessageHandler.ExecuteAndUndo executeAndUndo = getHandler(command)
                    .get().handle(data);
            executeAndUndo.undoCommand()
                    .ifPresent(undo -> history.put(requestId, executeAndUndo));
            BaseResponse response = executeAndUndo.executeCommand().execute();
            response.setRequestId(requestId);
            return response;
        } catch (ThemeEditorException ex) {
            getLogger().error(ex.getMessage(), ex);
            return new ErrorResponse(requestId, ex.getMessage());
        }
    }

    private Optional<MessageHandler> getHandler(String command) {
        return handlers.stream().filter(h -> h.getCommandName().equals(command))
                .findFirst();
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(ThemeEditorMessageHandler.class.getName());
    }

}
