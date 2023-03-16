package com.vaadin.base.devserver.themeeditor.messages;

public class RulesResponse extends BaseResponse {

    private String className;

    public RulesResponse() {

    }

    public RulesResponse(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
