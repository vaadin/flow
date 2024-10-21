package com.vaadin.flow.server.webpush;

public enum WebPushDir {
    AUTO("auto"),
    LTR("ltr"),
    RTL("rtl");

    private final String value;

    WebPushDir(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
