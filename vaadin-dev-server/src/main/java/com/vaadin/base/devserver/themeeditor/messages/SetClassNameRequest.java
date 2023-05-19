package com.vaadin.base.devserver.themeeditor.messages;

public class SetClassNameRequest extends BaseRequest {

    private String className;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
