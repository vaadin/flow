package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.StateNode;

import elemental.json.JsonObject;

public class NodeAttachChange extends NodeChange {

    public NodeAttachChange(StateNode node) {
        super(node);
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put("type", "attach");
    }
}
