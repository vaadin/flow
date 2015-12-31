package com.vaadin.client.communication.tree;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.JsArrayObject;
import com.vaadin.client.JsSet;
import com.vaadin.client.Profiler;
import com.vaadin.client.communication.tree.CallbackQueue.NodeChangeEvent;
import com.vaadin.client.communication.tree.Reactive.ReactiveValue;

import elemental.json.JsonObject;

public class ComputedTreeNodeProperty extends TreeNodeProperty {

    private final String code;

    private boolean dirty = true;

    private final JsArrayObject<HandlerRegistration> dependencies = JsArrayObject
            .create();

    private final Runnable dependencyListener = new Runnable() {
        @Override
        public void run() {
            Profiler.enter(
                    "ComputedTreeNodeProperty.dependencyListener.changeValue");

            Object oldOwnValue = getValue();
            dirty = true;
            clearAllDependencies();
            getOwner().getCallbackQueue().enqueue(new NodeChangeEvent() {
                @Override
                public JsonObject serialize() {
                    // Computed values are not serialized to the server
                    return null;
                }

                @Override
                public void dispatch() {
                    Profiler.enter(
                            "ComputedTreeNodeProperty.dependencyListener.dispatch");

                    JsSet<TreeNodePropertyValueChangeListener> listeners = getListeners();

                    if (listeners != null) {
                        Object newOwnValue = getValue();
                        JsSet.forEach(JsSet.create(listeners),
                                listener -> listener.changeValue(oldOwnValue,
                                        newOwnValue));
                    }

                    Profiler.leave(
                            "ComputedTreeNodeProperty.dependencyListener.dispatch");
                }
            });

            Profiler.leave(
                    "ComputedTreeNodeProperty.dependencyListener.changeValue");
        }
    };

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
        if (dirty) {
            compute();
        }
        return super.getValue();
    }

    private void clearAllDependencies() {
        for (int i = 0; i < dependencies.size(); i++) {
            HandlerRegistration dependency = dependencies.get(i);
            dependency.removeHandler();
        }
        dependencies.clear();
    }

    private void compute() {
        clearAllDependencies();

        JavaScriptObject context = getOwner().getProxy();

        // Should maybe be refactored to use Reactive.keepUpToDate
        JsSet<ReactiveValue> accessedProperies = Reactive
                .collectAccessedProperies(() -> {
                    JavaScriptObject newValue = TreeUpdater
                            .evalWithContextFactory(context, "return " + code);

                    dirty = false;
                    // Bypass own setValue since it always throws
                    super.setValue(newValue);
                });

        JsSet.forEach(accessedProperies, treeNodeProperty -> dependencies
                .add(treeNodeProperty.addReactiveListener(dependencyListener)));
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
