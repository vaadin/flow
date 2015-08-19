package com.vaadin.hummingbird.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EventBinding {

    private final String eventType;
    private final String methodName;
    private final List<String> params;

    public EventBinding(String eventType, String methodName, String[] params) {
        this.eventType = eventType;
        this.methodName = methodName;
        this.params = Collections.unmodifiableList(Arrays.asList(params));
    }

    public String getEventType() {
        return eventType;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getParams() {
        return params;
    }
}
