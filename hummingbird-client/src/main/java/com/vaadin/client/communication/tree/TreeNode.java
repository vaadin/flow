package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.FastStringMap;
import com.vaadin.client.JsSet;
import com.vaadin.client.communication.tree.CallbackQueue.NodeChangeEvent;

import elemental.js.json.JsJsonObject;
import elemental.json.JsonObject;
import elemental.json.JsonType;

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
                JsSet.forEach(JsSet.create(listeners),
                        listener -> listener.addProperty(name, property));
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

    private JsSet<TreeNodeChangeListener> listeners;
    private Map<Integer, Node> elements;

    private final ValueType valueType;

    public TreeNode(int id, TreeUpdater treeUpdater, ValueType valueType) {
        this(id, treeUpdater, JavaScriptObject.createObject(), valueType);
    }

    protected TreeNode(int id, TreeUpdater treeUpdater, JavaScriptObject proxy,
            ValueType valueType) {
        assert valueType.isObjectType();

        this.id = id;
        this.treeUpdater = treeUpdater;
        this.proxy = proxy;
        this.valueType = valueType;
        ((JsJsonObject) proxy.cast()).put(ID_PROPERTY, id);

        List<ComputedTreeNodeProperty> computedProperties = new ArrayList<>();

        Map<String, ValueType> typeProperties = valueType.getProperties();
        if (typeProperties != null) {
            Map<String, String> computed = valueType.getComputedProperties();

            for (Entry<String, ValueType> entry : typeProperties.entrySet()) {
                String name = entry.getKey();

                if (computed != null && computed.containsKey(name)) {
                    ComputedTreeNodeProperty property = addComputedProperty(
                            name, computed.get(name));
                    computedProperties.add(property);
                } else {
                    addProperty(
                            new TreeNodeProperty(this, name, entry.getValue()));
                }
            }
        }

        // Schedule flush of initial value to ensure bindings are set up
        Reactive.addFlushListener(() -> {
            for (ComputedTreeNodeProperty computedProperty : computedProperties) {
                computedProperty.compute();
            }
        });
    }

    public static int getProxyId(JavaScriptObject proxy) {
        JsonObject json = (JsJsonObject) proxy.cast();
        if (json.getType() != JsonType.OBJECT || !json.hasKey(ID_PROPERTY)) {
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
            listeners = JsSet.create();
        }
        listeners.add(listener);

        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                if (listeners == null) {
                    return;
                }
                listeners.delete(listener);
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
            ValueType propertyType = valueType.getProperties().get(name);
            property = new TreeNodeProperty(this, name, propertyType);
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

    private ComputedTreeNodeProperty addComputedProperty(String name,
            String code) {
        assert !properties.containsKey(name) : name
                + " is already registered as a property";

        ComputedTreeNodeProperty property = new ComputedTreeNodeProperty(this,
                name, code);
        addProperty(property);
        return property;
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

    public JsArrayString getPropertyNames() {
        return properties.getKeys();
    }

    public ValueType getValueType() {
        return valueType;
    }
}
