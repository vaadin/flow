package com.vaadin.base.devserver.themeeditor.messages;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse implements Serializable {

    public static final String COMMAND_NAME = "themeEditorResponse";

    public static final String CODE_OK = "ok";

    public static final String CODE_ERROR = "error";

    private String requestId;

    private String code;

    public BaseResponse() {
    }

    public BaseResponse(String requestId, String code) {
        this.requestId = requestId;
        this.code = code;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public static BaseResponse ok(String requestId) {
        return new BaseResponse(requestId, CODE_OK);
    }
}
