package com.vaadin.base.devserver.editor;

public enum Where {
    BEFORE, AFTER, INSIDE;

    public static Where from(String where) {
        for (Where w : values()) {
            if (w.name().equalsIgnoreCase(where)) {
                return w;
            }
        }
        return null;
    }
}