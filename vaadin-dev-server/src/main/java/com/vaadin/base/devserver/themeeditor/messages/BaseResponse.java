package com.vaadin.base.devserver.themeeditor.messages;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

import static com.vaadin.base.devserver.themeeditor.ThemeEditorCommand.CODE_OK;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse implements Serializable {

    private String requestId;

    public BaseResponse() {
    }

    public BaseResponse(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCode() {
        return CODE_OK;
    }

    public static BaseResponse ok() {
        return new BaseResponse();
    }
}
