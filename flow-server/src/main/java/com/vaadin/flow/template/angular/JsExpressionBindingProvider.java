/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.template.angular;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelMap;
import com.vaadin.flow.internal.nodefeature.TemplateMap;
import com.vaadin.flow.template.angular.model.BeanModelType;
import com.vaadin.flow.templatemodel.ModelType;

import elemental.json.JsonValue;

/**
 * A template binding value provider that produces dynamic value based on an
 * expression.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class JsExpressionBindingProvider extends AbstractBindingValueProvider {

    /**
     * Type identifier used for model data bindings in JSON messages.
     */
    public static final String TYPE = "model";

    private final String expression;

    private static final ScriptEngine SCRIPT_ENGINE = new ScriptEngineManager()
            .getEngineByName("nashorn");

    private static class ModelBindings implements Bindings {
        private final Bindings nashornGlobal = SCRIPT_ENGINE.createBindings();

        private final ModelMap map;
        private BeanModelType<?> type;

        private ModelBindings(ModelMap map, BeanModelType<?> type) {
            assert map != null;
            assert type != null;

            this.map = map;
            this.type = type;
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
                return type.hasProperty(key.toString());
            }
            return false;
        }

        @Override
        public Object get(Object key) {
            if ("nashorn.global".equals(key)) {
                /*
                 * Without this code engine will use {@code put} method to set
                 * global bindings. But the method is not implemented.
                 *
                 * Also it allows to avoid global bindings initialization for
                 * every Bindings instance.
                 */
                return nashornGlobal;
            }
            if (key instanceof String) {
                String propertyName = key.toString();

                ModelType propertyType = type.getPropertyType(propertyName);

                Serializable value = map.getValue(propertyName);

                return propertyType.modelToNashorn(value);
            }
            return null;
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Creates a binding value provider with the given JS {@code expression}.
     *
     * @param expression
     *            the binding expression, not null
     */
    public JsExpressionBindingProvider(String expression) {
        assert expression != null;
        this.expression = expression;
    }

    @Override
    public Object getValue(StateNode node) {
        ModelBindings bindings = new ModelBindings(ModelMap.get(node),
                node.getFeature(TemplateMap.class).getModelDescriptor());
        try {
            return SCRIPT_ENGINE.eval(expression, bindings);
        } catch (ScriptException e) {
            throw new IllegalStateException(String.format(
                    "Unable to evaluate expression '%s'", expression), e);
        }
    }

    @Override
    public JsonValue toJson() {
        return makeJsonObject(TYPE, expression);
    }

}
