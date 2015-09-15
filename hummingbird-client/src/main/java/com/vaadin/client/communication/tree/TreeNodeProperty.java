package com.vaadin.client.communication.tree;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.communication.tree.CallbackQueue.NodeChangeEvent;

import elemental.json.Json;
import elemental.json.JsonNumber;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class TreeNodeProperty {

    public interface TreeNodePropertyValueChangeListener {
        void changeValue(TreeNodeProperty property, Object oldValue);
    }

    public class ValueChangeEvent extends NodeChangeEvent {

        private final Object oldValue;

        public ValueChangeEvent(Object oldValue) {
            this.oldValue = oldValue;
        }

        @Override
        public void dispatch() {
            if (listeners != null) {
                for (TreeNodePropertyValueChangeListener listener : listeners) {
                    listener.changeValue(TreeNodeProperty.this, oldValue);
                }
            }
        }

        @Override
        public JsonObject serialize() {
            throw new RuntimeException("Not yet supported");
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
        owner.getCallbackQueue().enqueue(new ValueChangeEvent(oldValue));
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
