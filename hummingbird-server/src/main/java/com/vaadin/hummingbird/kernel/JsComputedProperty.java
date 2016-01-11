package com.vaadin.hummingbird.kernel;

import java.util.Objects;

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof JsComputedProperty) {
            JsComputedProperty that = (JsComputedProperty) obj;
            return getName().equals(that.getName())
                    && getClientCode().equals(that.getClientCode())
                    && type == that.type;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getClientCode(), type);
    }
}