package com.vaadin.hummingbird.kernel;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ModelBinding implements Binding {

    private final String binding;
    private final ModelContext context;

    public ModelBinding(String binding, ModelContext context) {
        this.binding = binding;
        this.context = context;
    }

    public String getBinding() {
        return binding;
    }

    @Override
    public Object getValue(StateNode node) {
        Function<String, Supplier<Object>> bindingFactory = context
                .getBindingFactory(node);
        return getValue(bindingFactory);
    }

    protected abstract Object getValue(
            Function<String, Supplier<Object>> bindingFactory);
}