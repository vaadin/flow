package com.vaadin.base.devserver.themeeditor.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ThemeEditorHistory {

    private static class UiHistory
            extends LinkedHashMap<String, MessageHandler.ExecuteAndUndo> {
        private static final int LIMIT = 100;

        @Override
        protected boolean removeEldestEntry(
                Map.Entry<String, MessageHandler.ExecuteAndUndo> eldest) {
            return size() > LIMIT;
        }
    };

    private static final Map<Integer, UiHistory> history = new HashMap<>();

    private Integer uiId;

    private ThemeEditorHistory() {

    }

    private ThemeEditorHistory(Integer uiId) {
        this.uiId = uiId;
    }

    public static ThemeEditorHistory forUi(Integer uiId) {
        return new ThemeEditorHistory(uiId);
    }

    public void clean() {
        history.remove(uiId);
    }

    public void put(String requestId,
            MessageHandler.ExecuteAndUndo executeAndUndo) {
        getUiHistory().put(requestId, executeAndUndo);
    }

    public boolean containsKey(String requestId) {
        if (requestId == null) {
            return false;
        }
        return getUiHistory().containsKey(requestId);
    }

    public MessageHandlerCommand getExecuteCommand(String requestId) {
        if (!containsKey(requestId)) {
            return null;
        }
        return getUiHistory().get(requestId).executeCommand();
    }

    public Optional<MessageHandlerCommand> getUndoCommand(String requestId) {
        if (!containsKey(requestId)) {
            return null;
        }
        return getUiHistory().get(requestId).undoCommand();
    }

    private UiHistory getUiHistory() {
        if (!history.containsKey(uiId)) {
            history.put(uiId, new UiHistory());
        }
        return history.get(uiId);
    }

}
