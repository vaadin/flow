package com.vaadin.client.communication.tree;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;

public class ComputedTreeNodeProperty extends TreeNodeProperty {

    private String code;

    public ComputedTreeNodeProperty(TreeNode owner, String name, String code) {
        super(owner, name);
        this.code = code;

        // Compute initial value
        compute();
    }

    @Override
    public JavaScriptObject getPropertyDescriptor() {
        JavaScriptObject descriptor = super.getPropertyDescriptor();
        removeSetter(descriptor);
        return descriptor;
    }

    @Override
    public Object getValue() {
        return super.getValue();
    }

    private void compute() {
        Map<String, JavaScriptObject> context = new HashMap<>();
        context.put("model", getOwner().getProxy());

        // TODO: make reactive
        JavaScriptObject newValue = TreeUpdater.evalWithContext(context,
                "return " + code);

        // Bypass own setValue since it always throws
        super.setValue(newValue);
    }

    @Override
    public void setValue(Object value) {
        throw new IllegalStateException(
                "Can't set value of read-only property " + getName());
    }

    private static native void removeSetter(JavaScriptObject descriptor)
    /*-{
        delete descriptor.set;
    }-*/;

}
