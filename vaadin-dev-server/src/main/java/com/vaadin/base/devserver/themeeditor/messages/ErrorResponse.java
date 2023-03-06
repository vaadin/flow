package com.vaadin.base.devserver.themeeditor.messages;

public class ErrorResponse extends BaseResponse {

    private String message;

    public ErrorResponse() {

    }

    public ErrorResponse(String requestId, String message) {
        super(requestId, CODE_ERROR);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
