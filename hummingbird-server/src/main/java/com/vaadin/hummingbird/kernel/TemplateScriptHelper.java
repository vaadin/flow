package com.vaadin.hummingbird.kernel;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class TemplateScriptHelper {
    @SuppressWarnings("restriction")
    private static final class StateNodeWrapper
            extends jdk.nashorn.api.scripting.AbstractJSObject {
        private StateNode node;

        public StateNodeWrapper(StateNode node) {
            this.node = node;
        }

        @Override
        public Object getMember(String name) {
            Object value = node.get(name);
            if (value instanceof StateNode) {
                StateNode childNode = (StateNode) value;
                return wrapNode(childNode);
            }
            return value;
        }
    }

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

    public static Object wrapNode(StateNode node) {
        return new StateNodeWrapper(node);
    }

}
