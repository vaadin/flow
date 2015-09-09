package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;

import elemental.json.Json;
import elemental.json.JsonNumber;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class TreeNodeProperty {

    public interface TreeNodePropertyValueChangeListener {
        void changeValue(TreeNodeProperty property, Object oldValue);
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
        return value;
    }

    public Object getProxyValue() {
        if (value instanceof TreeNode) {
            TreeNode node = (TreeNode) value;
            return node.getProxy();
        } else {
            return value;
        }
    }

    public void addPropertyChangeListener(
            TreeNodePropertyValueChangeListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }

    public void setValue(Object value) {
        final Object oldValue = this.value;
        if (value instanceof JavaScriptObject
                && ((JsonValue) value).getType() == JsonType.OBJECT) {
            throw new RuntimeException(
                    "Setting nodes from JavaScript is not yet supported");
        }

        this.value = value;
        owner.getCallbackQueue().enqueue(() -> {
            if (listeners == null || listeners.isEmpty()) {
                return;
            }
            for (TreeNodePropertyValueChangeListener listener : new ArrayList<>(
                    listeners)) {
                listener.changeValue(this, oldValue);
            }
        });
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
        return (int) ((JsonNumber) getValue()).getNumber();
    }

    public void setIntValue(int value) {
        setValue(Json.create(value));
    }
}
