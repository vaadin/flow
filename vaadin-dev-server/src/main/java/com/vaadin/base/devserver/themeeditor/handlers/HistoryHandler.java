package com.vaadin.base.devserver.themeeditor.handlers;

import com.vaadin.base.devserver.themeeditor.ThemeEditorCommand;
import com.vaadin.base.devserver.themeeditor.messages.HistoryRequest;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandler;
import com.vaadin.base.devserver.themeeditor.utils.MessageHandlerCommand;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorHistory;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.JsonObject;

import java.util.Optional;

public class HistoryHandler implements MessageHandler {

    @Override
    public ExecuteAndUndo handle(JsonObject data) {
        HistoryRequest request = JsonUtils.readToObject(data,
                HistoryRequest.class);
        ThemeEditorHistory history = ThemeEditorHistory
                .forUi(request.getUiId());
        if (request.getRedo() != null) {
            if (history.containsKey(request.getRedo())) {
                MessageHandlerCommand executeCommand = history
                        .getExecuteCommand(request.getRedo());
                return new ExecuteAndUndo(() -> executeCommand.execute(),
                        Optional.empty());
            } else {
                throw new ThemeEditorException(
                        "Given redo operation does not exist.");
            }
        }

        if (request.getUndo() != null) {
            if (history.containsKey(request.getUndo())) {
                Optional<MessageHandlerCommand> undoCommand = history
                        .getUndoCommand(request.getUndo());
                return new ExecuteAndUndo(() -> undoCommand.get().execute(),
                        Optional.empty());
            } else {
                throw new ThemeEditorException(
                        "Given undo operation does not exist.");
            }
        }

        throw new ThemeEditorException(
                "At least one of undo or redo must be specified.");
    }

    @Override
    public String getCommandName() {
        return ThemeEditorCommand.HISTORY;
    }

}
