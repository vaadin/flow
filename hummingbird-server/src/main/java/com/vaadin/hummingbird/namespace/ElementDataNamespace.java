package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;

public class ElementDataNamespace extends MapNamespace {

    private static final String TAG = "tag";

    public ElementDataNamespace(StateNode node) {
        super(node);
    }

    public void setTag(String tag) {
        put(TAG, tag);
    }

    public String getTag() {
        return (String) get(TAG);
    }
}
