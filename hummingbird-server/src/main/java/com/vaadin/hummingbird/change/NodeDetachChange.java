package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.StateNode;

import elemental.json.JsonObject;

public class NodeDetachChange extends NodeChange {
    public NodeDetachChange(StateNode node) {
        super(node);
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put("type", "detach");
    }
}
