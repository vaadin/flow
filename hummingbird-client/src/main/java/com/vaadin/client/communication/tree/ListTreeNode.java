package com.vaadin.client.communication.tree;

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.communication.tree.CallbackQueue.NodeChangeEvent;

import elemental.json.JsonObject;

public class ListTreeNode extends TreeNode {

    private final ArrayList<ArrayEventListener> listeners = new ArrayList<>();

    // Used for storing Java node instances if this is a node array
    private JsArrayObject<Object> nodes;

    // private final JsArrayObject<Object> proxy;

    public interface ArrayEventListener {
        void splice(ListTreeNode listTreeNode, int startIndex,
                JsArrayObject<Object> removed, JsArrayObject<Object> added);
    }

    public class ListTreeNodeSpliceEvent extends NodeChangeEvent {
        private final int startIndex;
        private final JsArrayObject<Object> removed;
        private final JsArrayObject<Object> added;

        public ListTreeNodeSpliceEvent(int startIndex,
                JsArrayObject<Object> removed, JsArrayObject<Object> added) {
            this.startIndex = startIndex;
            this.removed = removed;
            this.added = added;
        }

        @Override
        public void dispatch() {
            if (listeners != null) {
                for (ArrayEventListener arrayEventListener : listeners) {
                    arrayEventListener.splice(ListTreeNode.this, startIndex,
                            removed, added);
                }
            }
        }

        @Override
        public JsonObject serialize() {
            throw new RuntimeException("Not yet supported");
        }
    }

    public ListTreeNode(int id, TreeUpdater treeUpdater) {
        super(id, treeUpdater, createProxy());
    }

    private static native JsArrayObject<Object> createProxy()
    /*-{
        var a = [];
        a.splice = function(arguments) {
            throw "splice from JS is not yet implemented";
        }
        return a;
    }-*/;

    public void addArrayEventListener(ArrayEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public JsArrayObject<Object> getProxy() {
        return (JsArrayObject<Object>) super.getProxy();
    }

    public void splice(int index, int removeCount,
            JsArrayObject<Object> newValues) {

        boolean hasNewValues;
        if (newValues == null) {
            newValues = JavaScriptObject.createArray().cast();
            hasNewValues = false;
        } else {
            hasNewValues = newValues.size() != 0;
        }

        if (hasNewValues && getProxy().size() == 0
                && newValues.get(0) instanceof TreeNode && nodes == null) {
            nodes = JavaScriptObject.createArray().cast();
        }

        assert !hasNewValues || typesAreConsistent(newValues, nodes != null);

        if (nodes != null) {
            JsArrayObject<Object> removedNodes = doSplice(nodes, index,
                    removeCount, newValues);
            JsArrayObject<Object> newNodes = newValues;
            if (hasNewValues) {
                JsArrayObject<Object> newProxyValues = JavaScriptObject
                        .createArray().cast();
                for (int i = 0; i < newValues.size(); i++) {
                    newProxyValues
                            .add(((TreeNode) newValues.get(i)).getProxy());
                }
                newValues = newProxyValues;
            }
            doSplice(getProxy(), index, removeCount, newValues);
            onSplice(index, removedNodes, newNodes);
        } else {
            JsArrayObject<Object> removedValues = doSplice(getProxy(), index,
                    removeCount, newValues);
            onSplice(index, removedValues, newValues);
        }

    }

    private void onSplice(int startIndex, JsArrayObject<Object> removed,
            JsArrayObject<Object> added) {
        getTreeUpdater().getCallbackQueue().enqueue(
                new ListTreeNodeSpliceEvent(startIndex, removed, added));
    }

    public int getLength() {
        return getProxy().size();
    }

    public Object get(int index) {
        if (nodes != null) {
            Object value = nodes.get(index);
            if (value != null) {
                return value;
            }
        }
        return getProxy().get(index);
    }

    public static boolean typesAreConsistent(JsArrayObject<Object> newValues,
            boolean onlyNodes) {
        for (int i = 0; i < newValues.size(); i++) {
            Object value = newValues.get(i);
            // !! because of https://github.com/gwtproject/gwt/issues/9187
            assert onlyNodes == !!(value instanceof TreeNode);
        }
        return true;
    }

    public static native JsArrayObject<Object> doSplice(JsArrayObject<Object> a,
            int index, int removeCount, JsArrayObject<Object> newValues)
            /*-{
                var args = [index, removeCount];
                if (newValues) {
                    args = args.concat(newValues);
                }
                // Avoid the proxy implementation
                return Array.prototype.splice.apply(a, args);
            }-*/;

}
