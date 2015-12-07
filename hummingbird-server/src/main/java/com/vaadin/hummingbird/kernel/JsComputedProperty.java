package com.vaadin.hummingbird.kernel;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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
        String script = getClientCode();

        ScriptEngine engine = new ScriptEngineManager()
                .getEngineByName("nashorn");

        SimpleBindings bindings = new SimpleBindings();

        Class<?> modelType = (Class<?>) context.get(Model.class);
        Object model = Model.wrap(context, modelType);

        bindings.put("model", model);

        try {
            Object value = engine.eval(script, bindings);
            if (!type.isInstance(value)) {
                if (value instanceof Number) {
                    if (type == Integer.class || type == int.class) {
                        return Integer.valueOf(((Number) value).intValue());
                    }
                }
                throw new RuntimeException("Expected" + type + ", but got "
                        + value.getClass() + " from JS expression " + script);
            }

            return value;
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}