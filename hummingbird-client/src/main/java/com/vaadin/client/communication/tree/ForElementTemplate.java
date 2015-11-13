package com.vaadin.client.communication.tree;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.DomElement;
import com.vaadin.client.communication.tree.ListTreeNode.ArrayEventListener;
import com.vaadin.client.communication.tree.TreeNodeProperty.TreeNodePropertyValueChangeListener;

import elemental.json.JsonObject;

public class ForElementTemplate extends Template {
    private final TreeUpdater treeUpdater;
    private final Template childTemplate;
    private final String modelKey;
    private final String innerScope;
    int commentNodeIndex = -1;

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
    public Node createElement(TreeNode node, NodeContext outerContext) {
        // Creates anchor element
        Node commentNode = createCommentNode("for " + modelKey);

        outerContext.resolveListTreeNode(getModelKey())
                .addArrayEventListener(new ArrayEventListener() {

                    @Override
                    public void splice(ListTreeNode listTreeNode,
                            int startIndex, JsArrayObject<Object> removed,
                            JsArrayObject<Object> added) {
                        DomElement forNode = DomApi
                                .wrap(DomApi.wrap(commentNode).getParentNode());

                        if (commentNodeIndex == -1 || forNode.getChildNodes()
                                .get(commentNodeIndex) != commentNode) {
                            // Uses indexOf in order to avoid nextSibling
                            // because of
                            // https://github.com/Polymer/polymer/issues/2645

                            // Find commentNode again as it has moved
                            commentNodeIndex = forNode.getChildNodes()
                                    .indexOf(commentNode);

                        }

                        // Node to remove is "startIndex" positions forward from
                        // comment node

                        for (int i = 0; i < removed.size(); i++) {
                            int nodeIndex = commentNodeIndex + 1 + startIndex
                                    + i;

                            // Does not use nextSibling because of
                            // https://github.com/Polymer/polymer/issues/2645
                            Node removeNode = forNode.getChildNodes()
                                    .get(nodeIndex);
                            forNode.removeChild(removeNode);
                        }

                        for (int i = 0; i < added.size(); i++) {
                            TreeNode childNode = (TreeNode) added.get(i);
                            int insertIndex = commentNodeIndex + startIndex + i;
                            NodeContext innerContext = new NodeContext() {
                                @Override
                                public ListTreeNode resolveListTreeNode(
                                        String name) {
                                    if (isInnerScope(name)) {
                                        ListTreeNode listTreeNode = (ListTreeNode) childNode
                                                .getProperty(
                                                        getInnerProperty(name))
                                                .getValue();
                                        return listTreeNode;
                                    } else {
                                        return outerContext
                                                .resolveListTreeNode(name);
                                    }
                                }

                                @Override
                                public void listenToProperty(String name,
                                        TreeNodePropertyValueChangeListener listener) {
                                    if (isInnerScope(name)) {

                                        String innerProperty = getInnerProperty(
                                                name);
                                        TreeListenerHelper.addListener(
                                                childNode, innerProperty, true,
                                                listener);
                                    } else {
                                        outerContext.listenToProperty(name,
                                                listener);
                                    }
                                }

                                @Override
                                public Map<String, JavaScriptObject> buildEventHandlerContext() {
                                    Map<String, JavaScriptObject> contextMap = outerContext
                                            .buildEventHandlerContext();
                                    contextMap.put(getInnerScope(),
                                            childNode.getProxy());
                                    return contextMap;
                                }

                                @Override
                                public TreeNodeProperty getProperty(
                                        String name) {
                                    if (isInnerScope(name)) {
                                        return childNode.getProperty(
                                                getInnerProperty(name));
                                    } else {
                                        return outerContext.getProperty(name);
                                    }
                                }
                            };

                            Node child = treeUpdater.createElement(
                                    getChildTemplate(), childNode,
                                    innerContext);

                            // Does not use nextSibling because of
                            // https://github.com/Polymer/polymer/issues/2645
                            Node sibling = null;
                            int nodeAfterIndex = insertIndex + 1;
                            if (nodeAfterIndex < forNode.getChildNodes()
                                    .size()) {
                                sibling = forNode.getChildNodes()
                                        .get(nodeAfterIndex);
                            }

                            forNode.insertBefore(child, sibling);
                        }
                    }
                });

        return commentNode;
    }

    private static Node findNodeBefore(Node anchorNode, int index) {
        Node refChild = anchorNode;
        for (int i = 0; i < index; i++) {
            refChild = DomApi.wrap(refChild).getNextSibling();
        }
        return refChild;
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

    private boolean isInnerScope(String path) {
        return path.startsWith(getInnerScope());
    }

    private String getInnerProperty(String path) {
        return path.substring(getInnerScope().length() + 1);
    }

    private Template getChildTemplate() {
        return childTemplate;
    }
}