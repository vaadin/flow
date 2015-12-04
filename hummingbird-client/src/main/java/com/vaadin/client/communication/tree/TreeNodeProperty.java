package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.communication.tree.CallbackQueue.NodeChangeEvent;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class TreeNodeProperty {

    public interface TreeNodePropertyValueChangeListener {
        void changeValue(Object oldValue, Object newValue);
    }

    public class ValueChangeEvent extends NodeChangeEvent {

        private final Object oldValue;
        private final Object newValue;

        public ValueChangeEvent(Object oldValue, Object newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public void dispatch() {
            if (listeners != null) {
                for (TreeNodePropertyValueChangeListener listener : listeners) {
                    listener.changeValue(oldValue, newValue);
                }
            }
        }

        @Override
        public JsonObject serialize() {
            if (TreeNodeProperty.this instanceof ComputedTreeNodeProperty) {
                return null;
            }
            JsonObject json = Json.createObject();
            json.put("id", owner.getId());
            json.put("key", name);

            if (value instanceof TreeNode) {
                json.put("type", "putNode");
                json.put("value", ((TreeNode) value).getId());
            } else {
                json.put("type", "put");
                json.put("value", TreeUpdater.asJsonValue(value));
            }
            return json;
        }
    }

    private TreeNode owner;
    private String name;
    private Object value;

    private List<TreeNodePropertyValueChangeListener> listeners;

    public TreeNodeProperty(TreeNode owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public Object getValue() {
        Reactive.setAccessed(this);
        return value;
    }

    public Object getProxyValue() {
        Object value = getValue();
        if (value instanceof TreeNode) {
            TreeNode node = (TreeNode) value;
            return node.getProxy();
        } else {
            return value;
        }
    }

    public HandlerRegistration addPropertyChangeListener(
            TreeNodePropertyValueChangeListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);

        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                if (listeners != null) {
                    listeners.remove(listener);
                    if (listeners.isEmpty()) {
                        listeners = null;
                    }
                }
            }
        };
    }

    public void setValue(Object value) {
        final Object oldValue = this.value;
        if (value instanceof JavaScriptObject
                && ((JsonValue) value).getType() == JsonType.OBJECT) {
            int proxyId = TreeNode.getProxyId((JavaScriptObject) value);
            if (proxyId == -1) {
                throw new RuntimeException(
                        "Setting arbitrary objects from JavaScript is not yet supported");
            } else {
                value = owner.getTreeUpdater()
                        .getNode(Integer.valueOf(proxyId));
            }
        }

        this.value = value;
        owner.getCallbackQueue().enqueue(new ValueChangeEvent(oldValue, value));
    }

    public native JavaScriptObject getPropertyDescriptor()
    /*-{
        var self = this;
        return {
            enumerable: true,
            set: function(newValue) {self.@TreeNodeProperty::setValue(*)(newValue)},
            get: function() {return self.@TreeNodeProperty::getProxyValue()()}
        }
    }-*/;

    public int getIntValue() {
        return getAsInt(getValue());
    }

    private static native int getAsInt(Object object)
    /*-{
        return +object
    }-*/;

    public void setIntValue(int value) {
        setValue(Json.create(value));
    }

    public String getName() {
        return name;
    }

    public TreeNode getOwner() {
        return owner;
    }

    protected List<TreeNodePropertyValueChangeListener> getListeners() {
        return listeners;
    }
}
