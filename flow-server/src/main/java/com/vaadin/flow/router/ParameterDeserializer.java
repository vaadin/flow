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
package com.vaadin.flow.router;

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.flow.internal.ReflectTools;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parameter deserialization utility.
 *
 * @since 1.0
 */
public final class ParameterDeserializer {

    /**
     * Types supported by {@link #deserializeParameter(Class, String, String)}.
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
     * @param <T>
     *            the type to deserialize into
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
                    "Unsupported parameter type '%s' for class %s.",
                    parameterType, targetClass));
        }
    }

    /**
     * Deserializes the list of url segments to an instance of the parameter
     * type. Attempts to cast the first parameter segment to the parameter type
     * and returns null if the parameter list is empty.
     *
     * @param navigationTarget
     *            navigation target for which to deserialize parameters
     * @param urlParameters
     *            the list of url parameters to deserialize
     * @return the deserialized url parameter, can be {@code null}
     */
    public static Object deserializeUrlParameters(Class<?> navigationTarget,
            List<String> urlParameters) {
        if (urlParameters.isEmpty()) {
            return isAnnotatedParameter(navigationTarget,
                    WildcardParameter.class) ? "" : null;
        }
        Class<?> parameterType = getClassType(navigationTarget);
        if (isAnnotatedParameter(navigationTarget, WildcardParameter.class)) {
            validateWildcardType(navigationTarget, parameterType);
            return urlParameters.stream().collect(Collectors.joining("/"));
        }
        String parameter = urlParameters.get(0);
        return ParameterDeserializer.deserializeParameter(parameterType,
                parameter, navigationTarget.getName());
    }

    /**
     * Validate that we can support the given wildcard parameter type.
     *
     * @param navigationTarget
     *            navigation target class
     * @param parameterType
     *            parameter type to check validity for usage with wildcard
     */
    public static void validateWildcardType(Class<?> navigationTarget,
            Class<?> parameterType) {
        if (!parameterType.isAssignableFrom(String.class)) {
            throw new UnsupportedOperationException(
                    "Invalid wildcard parameter in class "
                            + navigationTarget.getName()
                            + ". Only String is supported for wildcard parameters.");
        }
    }

    /**
     * Verifies that the list of url parameters is valid for the given
     * navigation target.
     *
     * @param navigationTarget
     *            the navigation target to verify against
     * @param urlParameters
     *            the list of url parameters to verify
     * @return {@code true} if the parameters are valid, otherwise {@code false}
     */
    public static boolean verifyParameters(Class<?> navigationTarget,
            List<String> urlParameters) {
        if (!HasUrlParameter.class.isAssignableFrom(navigationTarget)) {
            throw new IllegalArgumentException(String.format(
                    "Given navigationTarget '%s' does not implement HasUrlParameter.",
                    navigationTarget.getName()));
        }

        Class<?> parameterType = getClassType(navigationTarget);

        if (supportedTypes.contains(parameterType)) {
            if (isAnnotatedParameter(navigationTarget,
                    WildcardParameter.class)) {
                return true;
            } else if (isAnnotatedParameter(navigationTarget,
                    OptionalParameter.class)) {
                return urlParameters.size() <= 1;
            }
            return urlParameters.size() == 1;
        }
        throw new UnsupportedOperationException(String.format(
                "Currently HasUrlParameter only supports the following parameter types: %s.",
                supportedTypes.stream().map(Class::getName)
                        .collect(Collectors.joining(", "))));
    }

    /**
     * Get the parameter type class.
     *
     * @param navigationTarget
     *            navigation target to get parameter type class for
     * @return parameter type class
     */
    public static Class<?> getClassType(Class<?> navigationTarget) {
        Type type = GenericTypeReflector.getTypeParameter(navigationTarget,
                HasUrlParameter.class.getTypeParameters()[0]);
        if (!(type instanceof Class<?>)) {
            throw new IllegalArgumentException(String.format(
                    "Parameter type of the given navigationTarget '%s' could not be resolved.",
                    navigationTarget.getName()));
        }
        return GenericTypeReflector.erase(type);
    }

    /**
     * Check if the parameter value is annotated as OptionalParameter.
     *
     * @param navigationTarget
     *            navigation target to check for optional
     * @param parameterAnnotation
     *            annotation to check parameter for
     * @return parameter is optional
     */
    public static boolean isAnnotatedParameter(Class<?> navigationTarget,
            Class<? extends Annotation> parameterAnnotation) {
        if (!HasUrlParameter.class.isAssignableFrom(navigationTarget)) {
            return false;
        }
        String methodName = "setParameter";
        assert methodName.equals(ReflectTools
                .getFunctionalMethod(HasUrlParameter.class).getName());

        // Raw method has no parameter annotations if compiled by Eclipse
        Type parameterType = GenericTypeReflector.getTypeParameter(
                navigationTarget,
                HasUrlParameter.class.getTypeParameters()[0]);
        Class<?> parameterClass = GenericTypeReflector.erase(parameterType);


        return Stream.of(navigationTarget.getMethods())
                .filter(method -> methodName.equals(method.getName()))
                .filter(method -> hasValidParameterTypes(method, parameterClass))
                .anyMatch(method -> method.getParameters()[1].isAnnotationPresent(parameterAnnotation));
    }

    private static boolean hasValidParameterTypes(Method method, Class<?> parameterClass) {
        return method.getParameterCount() == 2
                && method.getParameterTypes()[0] == BeforeEvent.class
                && method.getParameterTypes()[1].isAssignableFrom(parameterClass);
    }
}
