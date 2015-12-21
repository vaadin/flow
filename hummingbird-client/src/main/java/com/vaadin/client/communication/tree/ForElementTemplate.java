package com.vaadin.client.communication.tree;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.Profiler;
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
        Profiler.enter("ForElementTemplate.createElement");
        // Creates anchor element
        Node commentNode = createCommentNode("for " + modelKey);

        outerContext.resolveListTreeNode(getModelKey())
                .addArrayEventListener(new ArrayEventListener() {

                    @Override
                    public void splice(ListTreeNode listTreeNode,
                            int startIndex, JsArrayObject<Object> removed,
                            JsArrayObject<Object> added) {
                        Profiler.enter("ForElementTemplate splice");

                        DomElement forNode = DomApi
                                .wrap(DomApi.wrap(commentNode).getParentNode());

                        Profiler.enter(
                                "ForElementTemplate splice commentNodeIndex");
                        if (commentNodeIndex == -1 || forNode.getChildNodes()
                                .getItem(commentNodeIndex) != commentNode) {
                            // Uses indexOf in order to avoid nextSibling
                            // because of
                            // https://github.com/Polymer/polymer/issues/2645

                            // Find commentNode again as it has moved

                            commentNodeIndex = nodeListIndexOf(
                                    forNode.getChildNodes(), commentNode);
                        }
                        Profiler.leave(
                                "ForElementTemplate splice commentNodeIndex");

                        // Node to remove is "startIndex" positions forward from
                        // comment node

                        Profiler.enter("ForElementTemplate splice remove");
                        for (int i = 0; i < removed.size(); i++) {
                            int nodeIndex = commentNodeIndex + 1 + startIndex
                                    + i;

                            // Does not use nextSibling because of
                            // https://github.com/Polymer/polymer/issues/2645
                            Node removeNode = forNode.getChildNodes()
                                    .getItem(nodeIndex);
                            forNode.removeChild(removeNode);
                        }
                        Profiler.leave("ForElementTemplate splice remove");

                        Profiler.enter("ForElementTemplate splice add");
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

                                JavaScriptObject referenceOuterContext = null;
                                JavaScriptObject expressionContext = null;

                                @Override
                                public JavaScriptObject getExpressionContext() {
                                    JavaScriptObject currentOuterContext = outerContext
                                            .getExpressionContext();
                                    if (currentOuterContext != referenceOuterContext
                                            || expressionContext == null) {
                                        referenceOuterContext = currentOuterContext;
                                        expressionContext = createExpressionContext();
                                    }
                                    return expressionContext;
                                }

                                @Override
                                public JavaScriptObject createExpressionContext() {
                                    Profiler.enter(
                                            "ForElementTemplate.createExpressionContext");
                                    JavaScriptObject context = outerContext
                                            .createExpressionContext();
                                    TreeUpdater.addContextProperty(context,
                                            getInnerScope(), () -> {
                                        return childNode.getProxy();
                                    });
                                    Profiler.leave(
                                            "ForElementTemplate.createExpressionContext");
                                    return context;
                                }
                            };

                            Node child = treeUpdater.createElement(
                                    getChildTemplate(), childNode,
                                    innerContext);

                            Profiler.enter("ForElementTemplate splice insert");
                            // Does not use nextSibling because of
                            // https://github.com/Polymer/polymer/issues/2645
                            Node sibling = null;
                            int nodeAfterIndex = insertIndex + 1;
                            if (nodeAfterIndex < forNode.getChildNodes()
                                    .getLength()) {
                                sibling = forNode.getChildNodes()
                                        .getItem(nodeAfterIndex);
                            }

                            forNode.insertBefore(child, sibling);
                            Profiler.leave("ForElementTemplate splice insert");
                        }
                        Profiler.leave("ForElementTemplate splice add");

                        Profiler.leave("ForElementTemplate splice");
                    }
                });

        Profiler.leave("ForElementTemplate.createElement");
        return commentNode;
    }

    protected native int nodeListIndexOf(NodeList<Node> childNodes,
            Node childNode)
            /*-{
                return Array.prototype.indexOf.call(childNodes, childNode);
            }-*/;

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