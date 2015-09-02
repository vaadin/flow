package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Node;
import com.vaadin.client.communication.tree.ElementNotifier.ElementUpdater;
import com.vaadin.client.communication.tree.NodeListener.ListInsertChange;
import com.vaadin.client.communication.tree.NodeListener.ListInsertNodeChange;
import com.vaadin.client.communication.tree.NodeListener.ListRemoveChange;
import com.vaadin.client.communication.tree.NodeListener.PutChange;
import com.vaadin.client.communication.tree.NodeListener.PutNodeChange;
import com.vaadin.client.communication.tree.NodeListener.PutOverrideChange;
import com.vaadin.client.communication.tree.NodeListener.RemoveChange;

import elemental.json.JsonObject;

public class ForElementTemplate extends Template {
    private final TreeUpdater treeUpdater;
    private final Template childTemplate;
    private final String modelKey;
    private final String innerScope;

    private class ForAnchorListener implements ElementUpdater {
        private final Node anchorNode;
        private final NodeContext parentContext;

        public ForAnchorListener(Node anchorNode, NodeContext parentContext) {
            this.anchorNode = anchorNode;
            this.parentContext = parentContext;
        }

        @Override
        public void listInsertNode(String property,
                ListInsertNodeChange change) {
            if (!getModelKey().equals(property)) {
                return;
            }
            JsonObject childNode = treeUpdater.getNode(change.getValue());

            ElementNotifier notifier = new ElementNotifier(treeUpdater,
                    childNode, getInnerScope() + ".");
            Node child = treeUpdater
                    .createElement(getChildTemplate(), childNode,
                            new NodeContext(notifier,
                                    parentContext.getServerProxy(),
                                    parentContext.getModelProxy()));

            Node insertionPoint = findNodeBefore(change.getIndex());

            insertionPoint.getParentElement().insertAfter(child,
                    insertionPoint);
        }

        private Node findNodeBefore(int index) {
            Node refChild = anchorNode;
            for (int i = 0; i < index; i++) {
                refChild = refChild.getNextSibling();
            }
            return refChild;
        }

        @Override
        public void putNode(String property, PutNodeChange change) {
            // Don't care
        }

        @Override
        public void put(String property, PutChange change) {
            // Don't care
        }

        @Override
        public void listInsert(String property, ListInsertChange change) {
            // Don't care
        }

        @Override
        public void listRemove(String property, ListRemoveChange change) {
            if (!getModelKey().equals(property)) {
                return;
            }

            Node node = findNodeBefore(change.getIndex()).getNextSibling();

            node.removeFromParent();
        }

        @Override
        public void remove(String property, RemoveChange change) {
            // Don't care
        }

        @Override
        public void putOverride(String property, PutOverrideChange change) {
            // Don't care
        }
    }

    public ForElementTemplate(TreeUpdater treeUpdater,
            JsonObject templateDescription, int templateId) {
        super(templateId);
        this.treeUpdater = treeUpdater;
        modelKey = templateDescription.getString("modelKey");
        innerScope = templateDescription.getString("innerScope");

        childTemplate = new BoundElementTemplate(treeUpdater,
                templateDescription, templateId);
    }

    @Override
    public Node createElement(JsonObject node, NodeContext context) {
        // Creates anchor element
        Node commentNode = createCommentNode("for " + modelKey);
        context.getNotifier()
                .addUpdater(new ForAnchorListener(commentNode, context));
        return commentNode;
    }

    private static native Node createCommentNode(String comment)
    /*-{
        return $doc.createComment(comment);
    }-*/;

    private String getModelKey() {
        return modelKey;
    }

    private String getInnerScope() {
        return innerScope;
    }

    private Template getChildTemplate() {
        return childTemplate;
    }
}