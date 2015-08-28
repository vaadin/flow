package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.vaadin.client.communication.tree.ElementNotifier.ElementUpdater;
import com.vaadin.client.communication.tree.NodeListener.ListInsertChange;
import com.vaadin.client.communication.tree.NodeListener.ListInsertNodeChange;
import com.vaadin.client.communication.tree.NodeListener.ListRemoveChange;
import com.vaadin.client.communication.tree.NodeListener.PutChange;
import com.vaadin.client.communication.tree.NodeListener.PutNodeChange;
import com.vaadin.client.communication.tree.NodeListener.PutOverrideChange;
import com.vaadin.client.communication.tree.NodeListener.RemoveChange;

import elemental.json.JsonObject;

public class DynamicTextTemplate extends Template {
    private class TextUpdater implements ElementUpdater {
        private final Text textNode;

        public TextUpdater(Text textNode) {
            this.textNode = textNode;
        }

        @Override
        public void put(String scope, PutChange change) {
            if (scope.equals(binding)) {
                textNode.setData(change.getValue().asString());
            }
        }

        @Override
        public void remove(String scope, RemoveChange change) {
            if (scope.equals(binding)) {
                textNode.setData("");
            }
        }

        @Override
        public void putNode(String scope, PutNodeChange change) {
            // Don't care
        }

        @Override
        public void putOverride(String scope, PutOverrideChange change) {
            // Don't care
        }

        @Override
        public void listRemove(String scope, ListRemoveChange change) {
            // Don't care
        }

        @Override
        public void listInsert(String scope, ListInsertChange change) {
            // Don't care
        }

        @Override
        public void listInsertNode(String scope, ListInsertNodeChange change) {
            // Don't care
        }

    }

    private String binding;

    public DynamicTextTemplate(JsonObject templateDescription, int templateId) {
        super(templateId);
        binding = templateDescription.getString("binding");
    }

    @Override
    public Node createElement(JsonObject node, NodeContext context) {
        Text textNode = Document.get().createTextNode("");

        context.getNotifier().addUpdater(new TextUpdater(textNode));

        return textNode;
    }
}