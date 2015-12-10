package com.vaadin.hummingbird.kernel;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModelBinding implements Binding {

    private String binding;
    private ModelContext context;

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
        return TemplateScriptHelper.evaluateScript(bindingFactory, binding,
                Object.class);
    }

}
