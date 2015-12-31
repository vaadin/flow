package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.Profiler;
import com.vaadin.client.communication.DomApi;
import com.vaadin.client.communication.DomElement;
import com.vaadin.client.communication.tree.ListTreeNode.ArrayEventListener;

import elemental.json.JsonObject;

public class ForElementTemplate extends Template {
    private final TreeUpdater treeUpdater;
    private final Template childTemplate;
    private final String modelKey;
    private final String innerScope;
    int commentNodeIndex = -1;
    private String indexVariable;
    private String oddVariable;
    private String evenVariable;
    private String lastVariable;

    public ForElementTemplate(TreeUpdater treeUpdater,
            JsonObject templateDescription, int templateId) {
        super(templateId);
        this.treeUpdater = treeUpdater;
        modelKey = templateDescription.getString("modelKey");
        innerScope = templateDescription.getString("innerScope");
        indexVariable = templateDescription.getString("indexVar");
        evenVariable = templateDescription.getString("evenVar");
        oddVariable = templateDescription.getString("oddVar");
        lastVariable = templateDescription.getString("lastVar");

        childTemplate = new BoundElementTemplate(treeUpdater,
                templateDescription, templateId);
    }

    @Override
    public Node createElement(TreeNode node, NodeContext outerContext) {
        Profiler.enter("ForElementTemplate.createElement");
        // Creates anchor element
        Node commentNode = createCommentNode("for " + modelKey);

        ListTreeNode listNode = outerContext.resolveListTreeNode(getModelKey());
        if (listNode == null) {
            throw new RuntimeException(
                    "Unable to find model variable " + getModelKey());
        }
        listNode.addArrayEventListener(new ArrayEventListener() {

            @Override
            public void splice(ListTreeNode listTreeNode, int startIndex,
                    JsArrayObject<Object> removed,
                    JsArrayObject<Object> added) {
                Profiler.enter("ForElementTemplate splice");

                DomElement forNode = DomApi
                        .wrap(DomApi.wrap(commentNode).getParentNode());

                Profiler.enter("ForElementTemplate splice commentNodeIndex");
                if (commentNodeIndex == -1 || forNode.getChildNodes()
                        .getItem(commentNodeIndex) != commentNode) {
                    // Uses indexOf in order to avoid nextSibling
                    // because of
                    // https://github.com/Polymer/polymer/issues/2645

                    // Find commentNode again as it has moved

                    commentNodeIndex = nodeListIndexOf(forNode.getChildNodes(),
                            commentNode);
                }
                Profiler.leave("ForElementTemplate splice commentNodeIndex");

                // Node to remove is "startIndex" positions forward from
                // comment node

                Profiler.enter("ForElementTemplate splice remove");
                for (int i = 0; i < removed.size(); i++) {
                    int nodeIndex = commentNodeIndex + 1 + startIndex + i;

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
                    NodeContext innerContext = new NodeContext(outerContext) {
                        @Override
                        public ListTreeNode resolveListTreeNode(String name) {
                            if (isInnerScope(name)) {
                                ListTreeNode listTreeNode = (ListTreeNode) childNode
                                        .getProperty(getInnerProperty(name))
                                        .getValue();
                                return listTreeNode;
                            } else {
                                return outerContext.resolveListTreeNode(name);
                            }
                        }

                        @Override
                        public void populateEventHandlerContext(
                                JavaScriptObjectWithUsefulMethods context) {
                            outerContext.populateEventHandlerContext(context);
                            super.populateEventHandlerContext(context);
                            context.put(getInnerScope(), childNode.getProxy());
                            addIndexBasedVariables(context, listNode.getProxy(),
                                    childNode.getProxy(), indexVariable,
                                    evenVariable, oddVariable, lastVariable);
                        }

                        private final JavaScriptObjectWithUsefulMethods expressionContext = createExpressionContext(
                                outerContext.getExpressionContext());

                        {
                            expressionContext.put(getInnerScope(),
                                    childNode.getProxy());
                            addIndexBasedVariables(expressionContext,
                                    listNode.getProxy(), childNode.getProxy(),
                                    indexVariable, oddVariable, evenVariable,
                                    lastVariable);

                        }

                        @Override
                        public JavaScriptObject getExpressionContext() {
                            return expressionContext;
                        }
                    };

                    Node child = treeUpdater.createElement(getChildTemplate(),
                            childNode, innerContext);

                    Profiler.enter("ForElementTemplate splice insert");
                    // Does not use nextSibling because of
                    // https://github.com/Polymer/polymer/issues/2645
                    Node sibling = null;
                    int nodeAfterIndex = insertIndex + 1;
                    if (nodeAfterIndex < forNode.getChildNodes().getLength()) {
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

    private static native JavaScriptObjectWithUsefulMethods createExpressionContext(
            JavaScriptObject parentContext)
            /*-{
              return Object.create(parentContext);
            }-*/;

    private static native void addIndexBasedVariables(JavaScriptObject context,
            JsArrayObject<Object> outerScopeListProxy,
            JavaScriptObject innerScopeProxy, String indexVariable,
            String oddVariable, String evenVariable, String lastVariable)
            /*-{
                if (indexVariable != null) {
                    Object.defineProperty(context, indexVariable, {get: function(){
                        return outerScopeListProxy.indexOf(innerScopeProxy);
                    }});
                }
                if (evenVariable != null) {
                    Object.defineProperty(context, evenVariable, {get: function(){
                        return outerScopeListProxy.indexOf(innerScopeProxy) % 2 == 0;
                    }});
                }
                if (oddVariable != null) {
                    Object.defineProperty(context, oddVariable, {get: function(){
                        return outerScopeListProxy.indexOf(innerScopeProxy) % 2 != 0;
                    }});
                }
                if (lastVariable != null) {
                    Object.defineProperty(context, lastVariable, {get: function(){
                        return outerScopeListProxy.indexOf(innerScopeProxy)  == (outerScopeListProxy.length-1);
                    }});
                }
            }-*/;

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