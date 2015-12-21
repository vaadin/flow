package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.vaadin.client.Profiler;

import elemental.json.JsonObject;

public class DynamicTextTemplate extends Template {
    private String binding;

    public DynamicTextTemplate(JsonObject templateDescription, int templateId) {
        super(templateId);
        binding = templateDescription.getString("binding");
    }

    @Override
    public Node createElement(TreeNode node, NodeContext context) {
        Profiler.enter("DynamicTextTemplate.createElement");
        Text textNode = Document.get().createTextNode("");

        Reactive.keepUpToDate(() -> {
            Object value = BoundElementTemplate.evaluateExpression(binding,
                    context);
            updateValue(textNode, value);
        });

        Profiler.leave("DynamicTextTemplate.createElement");
        return textNode;
    }

    private void updateValue(Text textNode, Object value) {
        textNode.setData(value == null ? "" : value.toString());
    }
}