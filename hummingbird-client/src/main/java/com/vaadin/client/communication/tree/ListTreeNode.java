package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.JsSet;
import com.vaadin.client.Profiler;
import com.vaadin.client.communication.tree.CallbackQueue.NodeChangeEvent;
import com.vaadin.client.communication.tree.Reactive.ReactiveValue;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class ListTreeNode extends TreeNode implements ReactiveValue {

    private final JsSet<ArrayEventListener> listeners = JsSet.create();

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
            Profiler.enter("ListTreeNodeSpliceEvent.dispatch");
            if (listeners != null) {
                JsSet.forEach(JsSet.create(listeners), listener -> listener
                        .splice(ListTreeNode.this, startIndex, removed, added));
            }
            Profiler.leave("ListTreeNodeSpliceEvent.dispatch");
        }

        @Override
        public JsonObject serialize() {
            JsonObject json = Json.createObject();
            json.put("id", getId());
            json.put("type", "splice");
            json.put("index", startIndex);

            int removedCount = removed.size();
            if (removedCount != 0) {
                json.put("remove", removedCount);
            }

            int addedCount = added.size();
            if (addedCount != 0) {
                JsonArray addedJson = Json.createArray();
                String valueKey = "value";

                for (int i = 0; i < addedCount; i++) {
                    Object addedValue = added.get(i);
                    if (addedValue instanceof TreeNode) {
                        valueKey = "nodeValue";

                        addedJson.set(addedJson.length(),
                                ((TreeNode) addedValue).getId());
                    } else {
                        addedJson.set(addedJson.length(),
                                TreeUpdater.asJsonValue(addedValue));
                    }
                }

                json.put(valueKey, addedJson);
            }

            return json;
        }

    }

    public ListTreeNode(int id, TreeUpdater treeUpdater, ValueType valueType) {
        super(id, treeUpdater, JavaScriptObject.createArray(), valueType);
        assert valueType.isArrayType();

        populateProxy(getProxy());
    }

    private native void populateProxy(JsArrayObject<Object> a)
    /*-{
        a.splice = function(arguments) {
            throw "splice from JS is not yet implemented";
        }
        var self = this;
        a.indexOf = $entry(function(item) {
            return self.@ListTreeNode::indexOf(*)(item);
        });
        a.push = $entry(function() {
            self.@ListTreeNode::handleJsSplice(*)(a.length, 0, arguments);
            return a.length;
        });
    }-*/;

    private native int doIndexOf(JavaScriptObject list, Object item)
    /*-{
        // Avoid the proxy implementation
        return Array.prototype.indexOf.call(list, item);
    }-*/;

    public int indexOf(Object item) {
        Reactive.setAccessed(this);
        if (item instanceof TreeNode) {
            item = ((TreeNode) item).getProxy();
        }
        return doIndexOf(getProxy(), item);
    }

    private void handleJsSplice(int start, int deleteCount,
            JsArrayObject<Object> added) {
        if (added != null && added.size() != 0) {
            ValueType memberType = getValueType().getMemberType();
            if (memberType == getTreeUpdater().getTypeMap()
                    .get(ValueTypeMap.UNDEFINED)) {
                throw new RuntimeException(
                        "Can't add values if there's no defined member value type");
            }

            if (memberType.isObjectType()) {
                TreeUpdater treeUpdater = getTreeUpdater();

                // Unwrap or adopt all added nodes
                for (int i = 0; i < added.size(); i++) {
                    JavaScriptObject jsValue = (JavaScriptObject) added.get(i);
                    int proxyId = TreeNode.getProxyId(jsValue);
                    if (proxyId != -1) {
                        TreeNode node = treeUpdater.getNode(proxyId);
                        assert node != null;
                        assert node.getValueType() == memberType;

                        added.set(i, node);
                    } else {
                        TreeNode adoptedNode = treeUpdater.adoptNode(jsValue,
                                memberType);
                        added.set(i, adoptedNode);
                    }
                }
            }
        }

        splice(start, deleteCount, added);
    }

    public HandlerRegistration addArrayEventListener(
            ArrayEventListener listener) {
        listeners.add(listener);
        return () -> listeners.delete(listener);
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

    @Override
    public HandlerRegistration addReactiveListener(Runnable listener) {
        TreeUpdater.debug("addReactiveListener");
        return addArrayEventListener(new ArrayEventListener() {

            @Override
            public void splice(ListTreeNode listTreeNode, int startIndex,
                    JsArrayObject<Object> removed,
                    JsArrayObject<Object> added) {
                TreeUpdater.debug("addReactiveListener.splice");
                listener.run();

            }
        });
    }

}
