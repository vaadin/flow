package com.vaadin.hummingbird.kernel;

import com.vaadin.hummingbird.kernel.TemplateScriptHelper.NodeBindingFactory;

public class JsComputedProperty extends ComputedProperty {
    private Class<?> type;

    public JsComputedProperty(String name, String clientCode, Class<?> type) {
        super(name, clientCode);
        this.type = type;
    }

    @Override
    public Object compute(StateNode context) {
        NodeBindingFactory bindingFactory = new TemplateScriptHelper.NodeBindingFactory(
                context);

        return TemplateScriptHelper.evaluateScript(bindingFactory,
                getClientCode(), type);
    }
}