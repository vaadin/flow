package com.vaadin.hummingbird.kernel;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class TemplateScriptHelper {

    public static Object evaluateScript(Bindings bindings, String script,
            Class<?> resultType) {
        ScriptEngine engine = new ScriptEngineManager()
                .getEngineByName("nashorn");
        try {
            Object value = engine.eval(script, bindings);
            if (!resultType.isInstance(value)) {
                if (value instanceof Number) {
                    if (resultType == Integer.class
                            || resultType == int.class) {
                        return Integer.valueOf(((Number) value).intValue());
                    }
                }
                throw new RuntimeException("Expected " + resultType
                        + ", but got " + value.getClass()
                        + " from JS expression " + script);
            }

            return value;
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

}
