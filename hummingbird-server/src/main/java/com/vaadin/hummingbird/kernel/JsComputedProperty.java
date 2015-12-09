package com.vaadin.hummingbird.kernel;

import javax.script.SimpleBindings;

import com.vaadin.ui.Template.Model;

public class JsComputedProperty extends ComputedProperty {
    private Class<?> type;

    public JsComputedProperty(String name, String clientCode, Class<?> type) {
        super(name, clientCode);
        this.type = type;
    }

    @Override
    public Object compute(StateNode context) {
        ModelDescriptor modelDescriptor = context.get(ModelDescriptor.class,
                ModelDescriptor.class);
        Object model = Model.wrap(context, modelDescriptor.getModelType());

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("model", model);

        return TemplateScriptHelper.evaluateScript(bindings, getClientCode(),
                type);
    }
}