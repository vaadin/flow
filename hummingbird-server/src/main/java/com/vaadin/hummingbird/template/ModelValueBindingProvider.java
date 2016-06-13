/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.hummingbird.template;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.template.model.ModelPathResolver;

import elemental.json.JsonValue;

/**
 * A template binding value provider that produces dynamic value based on a
 * model.
 *
 * @author Vaadin Ltd
 */
public class ModelValueBindingProvider extends AbstractBindingValueProvider {

    /**
     * Type identifier used for model data bindings in JSON messages.
     */
    public static final String TYPE = "model";

    private final String key;

    private static final ScriptEngine SCRIPT_ENGINE = new ScriptEngineManager()
            .getEngineByName("nashorn");

    private static final Bindings NASHORN_GLOBAL = SCRIPT_ENGINE
            .createBindings();

    private static class ModelBindings implements Bindings {

        private final ModelMap map;

        private ModelBindings(ModelMap map) {
            this.map = map;
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsValue(Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<Object> values() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object put(String name, Object value) {
            throw new UnsupportedOperationException(
                    "Inline expression may not modify the global scope");
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> toMerge) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsKey(Object key) {
            assert key != null;
            if (key instanceof String) {
                return map.hasValue(key.toString());
            }
            return false;
        }

        @Override
        public Object get(Object key) {
            if ("nashorn.global".equals(key)) {
                // Without this code engine will use {@code put} method to set
                // global bindings. But the method is not implemented, so let's
                // just return the global bindings here.
                return NASHORN_GLOBAL;
            }
            if (key instanceof String) {
                return map.getValue(key.toString());
            }
            return null;
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Creates a binding value provider with the given {@code key}.
     * <p>
     * Value for the {@code key} is stored inside a {@link StateNode} directly
     * (via features) so only {@code key} is used to retrieve a dynamic value
     * from the node.
     *
     * @param key
     *            the key of the binding, not null
     */
    public ModelValueBindingProvider(String key) {
        assert key != null;
        this.key = key;
    }

    @Override
    public Object getValue(StateNode node) {
        ModelPathResolver resolver = new ModelPathResolver(key);
        ModelMap map = resolver.resolveModelMap(node);

        if (map == node.getFeature(ModelMap.class)) {
            ModelBindings bindings = new ModelBindings(map);
            try {
                return SCRIPT_ENGINE.eval(key, bindings);
            } catch (ScriptException e) {
                throw new IllegalStateException(String
                        .format("Unable to evaluate expression '%s'", key), e);
            }
        } else {
            return map.getValue(resolver.getPropertyName());
        }
    }

    @Override
    public JsonValue toJson() {
        return makeJsonObject(TYPE, key);
    }

}
