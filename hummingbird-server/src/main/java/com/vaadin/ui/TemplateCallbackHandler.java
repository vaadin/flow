package com.vaadin.ui;

import com.vaadin.hummingbird.kernel.StateNode;

import elemental.json.JsonArray;

@FunctionalInterface
public interface TemplateCallbackHandler {
    void handleCallback(StateNode node, String callbackName,
            JsonArray parameters, int promiseId);
}
