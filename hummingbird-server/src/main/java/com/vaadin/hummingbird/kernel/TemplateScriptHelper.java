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
            if (isArray()) {
                if ("length".equals(name)) {
                    return Integer.valueOf(((ListNode) node).size());
                } else if ("indexOf".equals(name)) {
                    return new jdk.nashorn.api.scripting.AbstractJSObject() {
                        @Override
                        public Object call(Object thiz, Object... args) {
                            return ((ListNode) node)
                                    .indexOf(wrapIfNode(args[0]));
                        }

                        @Override
                        public boolean isFunction() {
                            return true;
                        }
                    };
                }

            }
            Object value = node.get(name);

            return wrapIfNode(value);
        }

        @Override
        public boolean isArray() {
            return node instanceof ListNode;
        }

        @Override
        public Object getSlot(int index) {
            if (node instanceof ListNode) {
                Object value = ((ListNode) node).get(index);
                return wrapIfNode(value);
            } else {
                return super.getSlot(index);
            }
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
                    value = wrapIfNode(supplier.get());
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

    public static class NodeBindingFactory
            implements Function<String, Supplier<Object>> {
        private StateNode node;

        public NodeBindingFactory(StateNode node) {
            this.node = node;
        }

        @Override
        public Supplier<Object> apply(String name) {
            if (node.containsKey(name)) {
                return () -> {
                    return node.get(name);
                };
            } else {
                return null;
            }
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
                // XXX Is this acceptable if resultType is a primitive?
                return null;
            }
            if (value instanceof StateNodeWrapper) {
                StateNodeWrapper wrapper = (StateNodeWrapper) value;
                value = wrapper.node;
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

    private static Object wrapIfNode(Object value) {
        if (value instanceof StateNode) {
            return new StateNodeWrapper((StateNode) value);
        } else {
            return value;
        }
    }

}
