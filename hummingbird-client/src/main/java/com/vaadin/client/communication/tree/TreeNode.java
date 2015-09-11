package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.vaadin.client.FastStringMap;

public class TreeNode {
    public interface TreeNodeChangeListener {
        public void addProperty(String name, TreeNodeProperty property);

        public void addArray(String name, EventArray array);
    }

    private final JavaScriptObject proxy = JavaScriptObject.createObject();
    private final FastStringMap<TreeNodeProperty> properties = FastStringMap
            .create();
    private final CallbackQueue callbackQueue;
    private final int id;

    private List<TreeNodeChangeListener> listeners;
    private FastStringMap<EventArray> arrayProperties;
    private Map<Integer, Node> elements;

    public TreeNode(int id, CallbackQueue callbackQueue) {
        this.id = id;
        this.callbackQueue = callbackQueue;
    }

    public int getId() {
        return id;
    }

    public void addTreeNodeChangeListener(TreeNodeChangeListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }

    public JavaScriptObject getProxy() {
        return proxy;
    }

    public CallbackQueue getCallbackQueue() {
        return callbackQueue;
    }

    public TreeNodeProperty getProperty(String name) {
        TreeNodeProperty property = properties.get(name);
        // TODO check for existing array property
        if (property == null) {
            property = new TreeNodeProperty(this, name);
            properties.put(name, property);
            addPropertyDescriptor(proxy, name,
                    property.getPropertyDescriptor());
            final TreeNodeProperty finalProperty = property;
            callbackQueue.enqueue(() -> {
                if (listeners == null || listeners.isEmpty()) {
                    return;
                }
                for (TreeNodeChangeListener treeNodeChangeListener : new ArrayList<>(
                        listeners)) {
                    treeNodeChangeListener.addProperty(name, finalProperty);
                }
            });
        }
        return property;
    }

    private static native void addPropertyDescriptor(JavaScriptObject proxy,
            String name, JavaScriptObject descriptor)
            /*-{
                Object.defineProperty(proxy, name, descriptor);
            }-*/;

    public EventArray getArrayProperty(String name) {
        if (arrayProperties == null) {
            arrayProperties = FastStringMap.create();
        }

        // TODO check for existing non-array property
        EventArray eventArray = arrayProperties.get(name);
        if (eventArray == null) {
            eventArray = new EventArray(callbackQueue);
            arrayProperties.put(name, eventArray);

            addPropertyDescriptor(proxy, name, createEventArrayPd(eventArray));

            final EventArray finalArray = eventArray;
            callbackQueue.enqueue(() -> {
                if (listeners == null || listeners.isEmpty()) {
                    return;
                }
                for (TreeNodeChangeListener treeNodeChangeListener : new ArrayList<>(
                        listeners)) {
                    treeNodeChangeListener.addArray(name, finalArray);
                }
            });
        }
        return eventArray;
    }

    private static native JavaScriptObject createEventArrayPd(EventArray value)
    /*-{
        return {
          enumerable: false,
          value: value
        }
    }-*/;

    public void setElement(int templateId, Node element) {
        if (elements == null) {
            elements = new HashMap<>();
        }
        elements.put(Integer.valueOf(templateId), element);
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public Node getElement(int templateId) {
        if (elements == null) {
            return null;
        } else {
            return elements.get(Integer.valueOf(templateId));
        }
    }

}
