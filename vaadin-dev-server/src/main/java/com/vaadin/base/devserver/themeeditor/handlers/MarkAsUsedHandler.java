package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.BaseResponse;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.flow.internal.UsageStatistics;
import elemental.json.JsonObject;

import java.util.Optional;

public class MarkAsUsedHandler implements MessageHandler {

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        return new ExecuteAndUndo(() -> {
            UsageStatistics.markAsUsed("flow/ThemeEditor", null);
            return BaseResponse.ok();
        }, Optional.empty());
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.MARK_AS_USED;
    }
}
