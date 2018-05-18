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
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.internal.ReflectTools;

/**
 * A model type representing an immutable leaf value, e.g. strings, numbers or
 * booleans.
 *
 * @author Vaadin Ltd
 */
public class BasicModelType extends AbstractBasicModelType {

    static final Map<Class<?>, BasicModelType> TYPES = loadBasicTypes(
            BasicModelType::new);

    private BasicModelType(Class<?> type) {
        super(type);
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
        return Optional.ofNullable(TYPES.get(type));
    }

    @Override
    public Object modelToApplication(Serializable modelValue) {
        if (modelValue == null && getJavaType().isPrimitive()) {
            return ReflectTools.getPrimitiveDefaultValue(getJavaType());
        }
        if (modelValue == null) {
            return null;
        }

        Class<?> convertedJavaType = ReflectTools
                .convertPrimitiveType(getJavaType());

        // Number from the client is always double
        if (modelValue instanceof Double
                && convertedJavaType == Integer.class) {
            modelValue = Integer.valueOf(((Number) modelValue).intValue());
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

    @Override
    public Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter) {
        return (Serializable) applicationValue;
    }

}
