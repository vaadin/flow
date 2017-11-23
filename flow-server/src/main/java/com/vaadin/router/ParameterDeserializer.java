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
package com.vaadin.router;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Default parameter deserialization utility class.
 */
public final class ParameterDeserializer {

    /**
     * Types supported by the default deserializer.
     */
    public static final Set<Class<?>> supportedTypes = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList(Long.class,
                    Integer.class, String.class, Boolean.class)));

    private ParameterDeserializer() {
    }

    /**
     * Deserializer method for known parameter types.
     *
     * @param parameterType
     *            class to deserialize parameter as
     * @param parameter
     *            parameter to deserialize
     * @param targetClass
     *            name of handled class for exception usage
     * @return converted parameter as class if parameterType of supported type
     */
    public static <T> T deserializeParameter(Class<T> parameterType,
            String parameter, String targetClass) {
        if (parameterType.isAssignableFrom(String.class)) {
            return (T) parameter;
        } else if (parameterType.isAssignableFrom(Integer.class)) {
            return (T) Integer.valueOf(parameter);
        } else if (parameterType.isAssignableFrom(Long.class)) {
            return (T) Long.valueOf(parameter);
        } else if (parameterType.isAssignableFrom(Boolean.class)) {
            return (T) Boolean.valueOf(parameter);
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unsupported parameter type '%s'. Implement `deserializeUrlParameters` for class %s to handle deserialization.",
                    parameterType, targetClass));
        }
    }
}
