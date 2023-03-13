package com.vaadin.base.devserver.themeeditor.messages;

public class HistoryRequest extends BaseRequest {

    private String undo;

    private String redo;

    public HistoryRequest() {
    }

    public String getUndo() {
        return undo;
    }

    public void setUndo(String undo) {
        this.undo = undo;
    }

    public String getRedo() {
        return redo;
    }

    public void setRedo(String redo) {
        this.redo = redo;
    }

}
