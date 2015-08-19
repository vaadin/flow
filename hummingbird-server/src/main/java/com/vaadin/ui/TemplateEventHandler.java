package com.vaadin.ui;

import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.StateNode;

import elemental.json.JsonObject;

@FunctionalInterface
public interface TemplateEventHandler {
    void handleEvent(StateNode node, ElementTemplate template, String eventType,
            JsonObject eventData);
}
