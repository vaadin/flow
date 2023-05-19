package com.vaadin.base.devserver.themeeditor.messages;

import static com.vaadin.base.devserver.themeeditor.ThemeEditorCommand.CODE_ERROR;

public class ErrorResponse extends BaseResponse {

    private String message;

    public ErrorResponse() {

    }

    public ErrorResponse(String requestId, String message) {
        super(requestId);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getCode() {
        return CODE_ERROR;
    }
}
