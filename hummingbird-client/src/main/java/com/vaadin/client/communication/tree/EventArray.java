package com.vaadin.client.communication.tree;

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.JsArrayObject;

public class EventArray {
    public interface ArrayEventListener {
        void splice(EventArray eventArray, int startIndex,
                JsArrayObject<Object> removed, JsArrayObject<Object> added);
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

    public void splice(int index, int removeCount, Object... newValues) {
        boolean hasNewValues = newValues != null && newValues.length != 0;
        if (hasNewValues && proxy.size() == 0
                && newValues[0] instanceof TreeNode && nodes == null) {
            nodes = JavaScriptObject.createArray().cast();
        }

        assert!hasNewValues || typesAreConsistent(newValues, nodes != null);

        if (nodes != null) {
            JsArrayObject<Object> removedNodes = doSplice(nodes, index,
                    removeCount, newValues);
            Object[] newNodes = newValues;
            if (hasNewValues) {
                Object[] newProxyValues = new Object[newValues.length];
                for (int i = 0; i < newProxyValues.length; i++) {
                    newProxyValues[i] = ((TreeNode) newValues[i]).getProxy();
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

    private static boolean typesAreConsistent(Object[] newValues,
            boolean onlyNodes) {
        for (Object value : newValues) {
            // !! because of https://github.com/gwtproject/gwt/issues/9187
            assert onlyNodes == !!(value instanceof TreeNode);
        }
        return true;
    }

    public static native JsArrayObject<Object> doSplice(JsArrayObject<Object> a,
            int index, int removeCount, Object... newValues)
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
            Object[] addedObjects) {
        JsArrayObject<Object> added = asJsArray(addedObjects);
        callbackQueue.enqueue(() -> {
            if (listeners.isEmpty()) {
                return;
            }
            for (ArrayEventListener listener : new ArrayList<>(listeners)) {
                listener.splice(this, startIndex, removed, added);
            }
        });
    }

    private static native JsArrayObject<Object> asJsArray(Object[] value)
    /*-{
        return value;
    }-*/;

    public JavaScriptObject getProxy() {
        return proxy;
    }
}
