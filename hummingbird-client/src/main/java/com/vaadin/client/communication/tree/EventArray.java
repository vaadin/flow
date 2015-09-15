package com.vaadin.client.communication.tree;

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.communication.tree.CallbackQueue.NodeChangeEvent;

import elemental.json.JsonObject;

public class EventArray {
    public interface ArrayEventListener {
        void splice(EventArray eventArray, int startIndex,
                JsArrayObject<Object> removed, JsArrayObject<Object> added);
    }

    public class EventArraySpliceEvent extends NodeChangeEvent {
        private final int startIndex;
        private final JsArrayObject<Object> removed;
        private final JsArrayObject<Object> added;

        public EventArraySpliceEvent(int startIndex,
                JsArrayObject<Object> removed, JsArrayObject<Object> added) {
            this.startIndex = startIndex;
            this.removed = removed;
            this.added = added;
        }

        @Override
        public void dispatch() {
            if (listeners != null) {
                for (ArrayEventListener arrayEventListener : listeners) {
                    arrayEventListener.splice(EventArray.this, startIndex,
                            removed, added);
                }
            }
        }

        @Override
        public JsonObject serialize() {
            throw new RuntimeException("Not yet supported");
        }
    }

    private final CallbackQueue callbackQueue;
    private final JsArrayObject<Object> proxy;
    private final ArrayList<ArrayEventListener> listeners = new ArrayList<>();

    // Used for storing Java node instances if this is a node array
    private JsArrayObject<Object> nodes;

    public EventArray(CallbackQueue callbackQueue) {
        this.callbackQueue = callbackQueue;
        proxy = createProxy(this);
    }

    public void addArrayEventListener(ArrayEventListener listener) {
        listeners.add(listener);
    }

    public Object get(int index) {
        if (nodes != null) {
            Object value = nodes.get(index);
            if (value != null) {
                return value;
            }
        }
        return proxy.get(index);
    }

    public int getLength() {
        return proxy.size();
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

        if (hasNewValues && proxy.size() == 0
                && newValues.get(0) instanceof TreeNode && nodes == null) {
            nodes = JavaScriptObject.createArray().cast();
        }

        assert!hasNewValues || typesAreConsistent(newValues, nodes != null);

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
            doSplice(proxy, index, removeCount, newValues);
            onSplice(index, removedNodes, newNodes);
        } else {
            JsArrayObject<Object> removedValues = doSplice(proxy, index,
                    removeCount, newValues);
            onSplice(index, removedValues, newValues);
        }

    }

    private static boolean typesAreConsistent(JsArrayObject<Object> newValues,
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

    private static native JsArrayObject<Object> createProxy(
            EventArray eventArray)
            /*-{
                var a = [];
                a.splice = function(arguments) {
                    throw "splice from JS is not yet implemented";
                }
                return a;
            }-*/;

    private void onSplice(int startIndex, JsArrayObject<Object> removed,
            JsArrayObject<Object> added) {
        callbackQueue
                .enqueue(new EventArraySpliceEvent(startIndex, removed, added));
    }

    public JavaScriptObject getProxy() {
        return proxy;
    }
}
