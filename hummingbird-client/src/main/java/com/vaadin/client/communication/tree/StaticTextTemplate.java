package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;

import elemental.json.JsonObject;

public class StaticTextTemplate extends Template {

    private String content;

    public StaticTextTemplate(JsonObject templateDescription,
            int templateId) {
        super(templateId);
        content = templateDescription.getString("content");
    }

    @Override
    public Node createElement(TreeNode node, NodeContext context) {
        return Document.get().createTextNode(content);
    }

}