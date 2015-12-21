package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.vaadin.client.Profiler;

import elemental.json.JsonObject;

public class StaticTextTemplate extends Template {

    private String content;

    public StaticTextTemplate(JsonObject templateDescription, int templateId) {
        super(templateId);
        content = templateDescription.getString("content");
    }

    @Override
    public Node createElement(TreeNode node, NodeContext context) {
        Profiler.enter("StaticTextTemplate.createElement");
        Text element = Document.get().createTextNode(content);
        Profiler.leave("StaticTextTemplate.createElement");
        return element;
    }

}