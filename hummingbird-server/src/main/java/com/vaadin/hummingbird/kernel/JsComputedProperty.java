package com.vaadin.hummingbird.kernel;

import javax.script.SimpleBindings;

public class JsComputedProperty extends ComputedProperty {
    private Class<?> type;

    public JsComputedProperty(String name, String clientCode, Class<?> type) {
        super(name, clientCode);
        this.type = type;
    }

    @Override
    public Object compute(StateNode context) {
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("model", TemplateScriptHelper.wrapNode(context));

        return TemplateScriptHelper.evaluateScript(bindings, getClientCode(),
                type);
    }
}