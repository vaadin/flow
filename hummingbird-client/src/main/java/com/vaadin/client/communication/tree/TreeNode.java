package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.FastStringMap;
import com.vaadin.client.communication.tree.CallbackQueue.NodeChangeEvent;

import elemental.js.json.JsJsonObject;
import elemental.json.JsonObject;

public class TreeNode {
    private static final String ID_PROPERTY = "$hid";

    public interface TreeNodeChangeListener {
        public void addProperty(String name, TreeNodeProperty property);
    }

    public class PropertyAddEvent extends NodeChangeEvent {
        private final String name;
        private final TreeNodeProperty property;

        public PropertyAddEvent(String name, TreeNodeProperty property) {
            this.name = name;
            this.property = property;
        }

        @Override
        public void dispatch() {
            if (listeners != null) {
                for (TreeNodeChangeListener treeNodeChangeListener : listeners) {
                    treeNodeChangeListener.addProperty(name, property);
                }
            }
        }

        @Override
        public JsonObject serialize() {
            // Event is not serialized
            return null;
        }
    }

    private final JavaScriptObject proxy;
    private final FastStringMap<TreeNodeProperty> properties = FastStringMap
            .create();
    private final TreeUpdater treeUpdater;
    private final int id;

    private List<TreeNodeChangeListener> listeners;
    private Map<Integer, Node> elements;

    public TreeNode(int id, TreeUpdater treeUpdater) {
        this(id, treeUpdater, JavaScriptObject.createObject());
    }

    protected TreeNode(int id, TreeUpdater treeUpdater,
            JavaScriptObject proxy) {
        this.id = id;
        this.treeUpdater = treeUpdater;
        this.proxy = proxy;
        ((JsJsonObject) proxy.cast()).put(ID_PROPERTY, id);
    }

    public static int getProxyId(JavaScriptObject proxy) {
        JsonObject json = (JsJsonObject) proxy.cast();
        if (!json.hasKey(ID_PROPERTY)) {
            return -1;
        }
        return (int) json.getNumber(ID_PROPERTY);
    }

    public int getId() {
        return id;
    }

    public HandlerRegistration addTreeNodeChangeListener(
            TreeNodeChangeListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);

        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                if (listeners == null) {
                    return;
                }
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    listeners = null;
                }
            }
        };
    }

    public JavaScriptObject getProxy() {
        return proxy;
    }

    public CallbackQueue getCallbackQueue() {
        return getTreeUpdater().getCallbackQueue();
    }

    public TreeNodeProperty getProperty(String name) {
        TreeNodeProperty property = properties.get(name);
        // TODO check for existing array property
        if (property == null) {
            property = new TreeNodeProperty(this, name);
            addProperty(property);
        }
        return property;
    }

    private void addProperty(TreeNodeProperty property) {
        String name = property.getName();
        properties.put(name, property);
        addPropertyDescriptor(proxy, name, property.getPropertyDescriptor());
        getCallbackQueue().enqueue(new PropertyAddEvent(name, property));
    }

    public void addComputedProperty(String name, String code) {
        assert !properties.containsKey(name) : name
                + " is already registered as a property";

        addProperty(new ComputedTreeNodeProperty(this, name, code));
    }

    private static native void addPropertyDescriptor(JavaScriptObject proxy,
            String name, JavaScriptObject descriptor)
            /*-{
                Object.defineProperty(proxy, name, descriptor);
            }-*/;

    private static native JavaScriptObject createEventArrayPd(
            JavaScriptObject proxy)
            /*-{
                return {
                  enumerable: true,
                  value: proxy
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

    public TreeUpdater getTreeUpdater() {
        return treeUpdater;
    }

}
