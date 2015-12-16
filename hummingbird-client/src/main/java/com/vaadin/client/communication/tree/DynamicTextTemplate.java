package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

import elemental.json.JsonObject;

public class DynamicTextTemplate extends Template {
    private String binding;

    public DynamicTextTemplate(JsonObject templateDescription, int templateId) {
        super(templateId);
        binding = templateDescription.getString("binding");
    }

    @Override
    public Node createElement(TreeNode node, NodeContext context) {
        Text textNode = Document.get().createTextNode("");

        Reactive.keepUpToDate(() -> {
            TreeNodeProperty p = context.getProperty(binding);
            Object value = p.getValue();
            updateValue(textNode, value);
        });

        return textNode;
    }

    private void updateValue(Text textNode, Object value) {
        textNode.setData(value == null ? "" : value.toString());
    }
}