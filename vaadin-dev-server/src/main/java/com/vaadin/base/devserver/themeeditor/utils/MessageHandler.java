package com.vaadin.base.devserver.themeeditor.utils;

import elemental.json.JsonObject;

import java.util.Optional;

public interface MessageHandler {

    record ExecuteAndUndo(MessageHandlerCommand executeCommand, Optional<MessageHandlerCommand> undoCommand) { }

    ExecuteAndUndo handle(JsonObject data);

    String getCommandName();

}
