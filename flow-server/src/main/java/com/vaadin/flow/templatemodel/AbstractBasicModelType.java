/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;

import elemental.json.Json;
import elemental.json.JsonValue;

/**
 * Common abstract class with generic functionality for basic mode type.
 *
 * @param <T>
 *            the (basic) Java type used by this model type
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public abstract class AbstractBasicModelType<T> implements ModelType {

    private final Class<T> type;

    protected AbstractBasicModelType(Class<T> type) {
        this.type = type;
    }

    @Override
    public boolean accepts(Type applicationType) {
        if (type.isPrimitive()
                && ReflectTools.convertPrimitiveType(type) == applicationType) {
            return true;
        }
        return type == applicationType;
    }

    @Override
    public Class<T> getJavaType() {
        return type;
    }

    @Override
    public JsonValue toJson() {
        return Json.create(type.getSimpleName());
    }

    @Override
    public void createInitialValue(StateNode node, String property) {
        ElementPropertyMap feature = node.getFeature(ElementPropertyMap.class);
        if (!feature.hasProperty(property)) {
            feature.setProperty(property,
                    (Serializable) modelToApplication(null));
        }
    }

    /**
     * Converts the given model value to the application type of this model
     * type. The conversion automatically handles default values for primitive
     * types and conversion of client-originiated numbers to the expected Java
     * number type.
     *
     * @param modelValue
     *            the model value to convert
     * @return the converted value, not <code>null</code> if the application
     *         type is a primitive
     */
    protected Object convertToApplication(Serializable modelValue) {
        if (modelValue == null && getJavaType().isPrimitive()) {
            return ReflectTools.getPrimitiveDefaultValue(getJavaType());
        }
        if (modelValue == null) {
            return null;
        }

        Class<?> convertedJavaType = ReflectTools
                .convertPrimitiveType(getJavaType());

        // Numeric value from the client is always Double
        if (modelValue instanceof Double
                && convertedJavaType == Integer.class) {
            modelValue = Integer.valueOf(((Double) modelValue).intValue());
        }

        if (convertedJavaType == modelValue.getClass()) {
            return modelValue;
        } else {
            throw new IllegalArgumentException(String.format(
                    "The stored model value '%s' type '%s' "
                            + "cannot be used as a type for a model property with type '%s'",
                    modelValue, modelValue.getClass().getName(),
                    getJavaType().getName()));
        }
    }

    protected static <M> Map<Class<?>, M> loadBasicTypes(
            Function<Class<?>, M> factory) {
        Map<Class<?>, M> map = Stream
                .of(int.class, Integer.class, boolean.class, Boolean.class,
                        double.class, Double.class, String.class)
                .collect(Collectors.toMap(Function.identity(), factory));

        // Make sure each type has a unique getSimpleName value since it's used
        // as an identifier in JSON messages
        assert map.keySet().stream().map(Class::getSimpleName).distinct()
                .count() == map.size();
        return Collections.unmodifiableMap(map);
    }
}
