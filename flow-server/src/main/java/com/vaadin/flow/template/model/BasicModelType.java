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
package com.vaadin.flow.template.model;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.util.ReflectTools;

import elemental.json.Json;
import elemental.json.JsonValue;

/**
 * A model type representing an immutable leaf value, e.g. strings, numbers or
 * booleans.
 *
 * @author Vaadin Ltd
 */
public class BasicModelType implements ModelType {
    static final Map<Class<?>, BasicModelType> types = new HashMap<>();

    static {
        Stream.of(int.class, Integer.class, boolean.class, Boolean.class,
                double.class, Double.class, String.class)
                .forEach(type -> types.put(type, new BasicModelType(type)));

        // Make sure each type has a unique getSimpleName value since it's used
        // as an identifier in JSON messages
        assert types.keySet().stream().map(Class::getSimpleName).distinct()
                .count() == types.size();
    }

    private final Class<?> type;

    private BasicModelType(Class<?> type) {
        this.type = type;
    }

    /**
     * Gets the basic model type definition for the given Java class.
     *
     * @param type
     *            the Java class to find a basic model type for
     * @return the basic model type, or an empty optional if the provided type
     *         is not a basic type
     */
    public static Optional<ModelType> get(Class<?> type) {
        return Optional.ofNullable(types.get(type));
    }

    @Override
    public Object modelToApplication(Serializable modelValue) {
        if (modelValue == null && type.isPrimitive()) {
            return ReflectTools.getPrimitiveDefaultValue(type);
        } else {
            return modelValue;
        }
    }

    @Override
    public Object modelToNashorn(Serializable modelValue) {
        return modelToApplication(modelValue);
    }

    @Override
    public Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        return (Serializable) applicationValue;
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
    public Type getJavaType() {
        return type;
    }

    @Override
    public JsonValue toJson() {
        return Json.create(type.getSimpleName());
    }
}
