package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.LoadPreviewResponse;
import com.vaadin.base.devserver.themeeditor.utils.HasThemeModifier;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import elemental.json.JsonObject;

import java.util.Optional;

public class LoadPreviewHandler implements MessageHandler {

    private final HasThemeModifier hasThemeModifier;

    public LoadPreviewHandler(HasThemeModifier hasThemeModifier) {
        this.hasThemeModifier = hasThemeModifier;
    }

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        return new ExecuteAndUndo(() -> {
            String css = hasThemeModifier.getThemeModifier().getCss();
            return new LoadPreviewResponse(css);
        }, Optional.empty());
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.LOAD_PREVIEW;
    }
}
