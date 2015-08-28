package com.vaadin.hummingbird.parser;

public class EventBinding {

    private final String eventType;
    private final String eventHandler;

    public EventBinding(String eventType, String eventHandler) {
        this.eventType = eventType;
        this.eventHandler = eventHandler;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventHandler() {
        return eventHandler;
    }
}
