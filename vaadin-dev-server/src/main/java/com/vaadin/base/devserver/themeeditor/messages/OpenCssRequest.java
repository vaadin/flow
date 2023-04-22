package com.vaadin.base.devserver.themeeditor.messages;

public class OpenCssRequest extends BaseRequest {

    private String selector;

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }
}
