package com.vaadin.hummingbird.kernel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
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

    private static class DynamicBindings implements Bindings {
        private Function<String, Supplier<Object>> bindingFactory;
        private Map<String, Object> values = new HashMap<>();

        public DynamicBindings(
                Function<String, Supplier<Object>> bindingFactory) {
            this.bindingFactory = bindingFactory;
        }

        @Override
        public Object put(String name, Object value) {
            throw new IllegalStateException(
                    "Inline expression may not modify the global scope");
        }

        @Override
        public boolean containsKey(Object key) {
            return true;
        }

        @Override
        public Object get(Object key) {
            if ("nashorn.global".equals(key)) {
                return nashornGlobal;
            }
            if (!values.containsKey(key)) {
                Supplier<Object> supplier = bindingFactory.apply((String) key);
                Object value;
                if (supplier != null) {
                    value = supplier.get();
                } else {
                    value = null;
                }
                values.put((String) key, value);
            }

            return values.get(key);
        }

        @Override
        public Object remove(Object key) {
            throw new RuntimeException();
        }

        @Override
        public int size() {
            throw new RuntimeException();
        }

        @Override
        public boolean isEmpty() {
            throw new RuntimeException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new RuntimeException();
        }

        @Override
        public void clear() {
            throw new RuntimeException();
        }

        @Override
        public Set<String> keySet() {
            throw new RuntimeException();
        }

        @Override
        public Collection<Object> values() {
            throw new RuntimeException();
        }

        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet() {
            throw new RuntimeException();
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> toMerge) {
            throw new RuntimeException();
        }
    }

    public static Object evaluateScript(
            Function<String, Supplier<Object>> bindingFactory, String script,
            Class<?> resultType) {
        return evaluateScript(new DynamicBindings(bindingFactory), script,
                resultType);
    }

    private static final ScriptEngine engine = new ScriptEngineManager()
            .getEngineByName("nashorn");
    private static final Bindings nashornGlobal = engine.createBindings();

    private static final ConcurrentHashMap<String, CompiledScript> compileCache = new ConcurrentHashMap<>();

    public static Object evaluateScript(Bindings bindings, String script,
            Class<?> resultType) {
        try {
            CompiledScript compiled = compileCache.computeIfAbsent(script,
                    string -> {
                        try {
                            return ((Compilable) engine).compile(string);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

            Object value = compiled.eval(bindings);
            if (value == null) {
                // XXX Is this acceptable it resultType is a primitive?
                return null;
            }
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
