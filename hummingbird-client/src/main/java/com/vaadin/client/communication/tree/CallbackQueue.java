package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.List;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class CallbackQueue {

    public abstract static class NodeChangeEvent {
        public abstract void dispatch();

        public abstract JsonObject serialize();
    }

    private List<NodeChangeEvent> events = new ArrayList<>();

    public void enqueue(NodeChangeEvent event) {
        events.add(event);
    }

    public void flush(JsonArray serializeTo) {
        List<NodeChangeEvent> callbacks = events;
        events = new ArrayList<>();
        for (NodeChangeEvent event : callbacks) {
            event.dispatch();
            if (serializeTo != null) {
                JsonObject serialize = event.serialize();
                if (serialize != null) {
                    serializeTo.set(serializeTo.length(), serialize);
                }
            }
        }
    }
}
