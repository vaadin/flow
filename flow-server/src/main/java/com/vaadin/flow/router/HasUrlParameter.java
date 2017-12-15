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
package com.vaadin.flow.router;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.util.ReflectTools;

/**
 * Interface for defining url parameters for navigation targets for use in
 * routing.
 * 
 * @author Vaadin Ltd.
 *
 * @param <T>
 *            type parameter type
 */
@FunctionalInterface
public interface HasUrlParameter<T> {

    /**
     * Method that is called automatically when navigating to the target that
     * implements this interface.
     *
     * @param event
     *            the navigation event that caused the call to this method
     * @param parameter
     *            the resolved url parameter
     */
    void setParameter(BeforeNavigationEvent event, T parameter);

    /**
     * Method used to deserialize the list of url segments to an instance of the
     * parameter type. This method can be overridden to support more complex
     * objects as an url parameter. By default this method attempts to cast the
     * first parameter segment to the parameter type and if the parameter list
     * is empty returns null.
     *
     * @param urlParameters
     *            the list of url parameters to deserialize
     * @return the deserialized url parameter, can be {@code null}
     */
    @SuppressWarnings("unchecked")
    default T deserializeUrlParameters(List<String> urlParameters) {
        if (urlParameters.isEmpty()) {
            return isAnnotatedParameter(this.getClass(),
                    WildcardParameter.class) ? (T) "" : null;
        }
        Class<?> parameterType = getClassType(this.getClass());
        if (isAnnotatedParameter(this.getClass(), WildcardParameter.class)) {
            validateWildcardType(this.getClass(), parameterType);
            return (T) urlParameters.stream().collect(Collectors.joining("/"));
        }
        String parameter = urlParameters.get(0);
        return ParameterDeserializer.deserializeParameter(
                (Class<T>) parameterType, parameter, this.getClass().getName());
    }

    /**
     * Validate that we can support the given wildcard parameter type.
     * 
     * @param navigationTarget
     *            navigation target class
     * @param parameterType
     *            parameter type to check validity for usage with wildcard
     */
    static void validateWildcardType(Class<?> navigationTarget,
            Class<?> parameterType) {
        if (!parameterType.isAssignableFrom(String.class)) {
            throw new UnsupportedOperationException(
                    "Wildcard parameter can only be for String type by default. Implement `deserializeUrlParameters` for class "
                            + navigationTarget.getName());
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
    static boolean verifyParameters(Class<?> navigationTarget,
            List<String> urlParameters) {
        if (!HasUrlParameter.class.isAssignableFrom(navigationTarget)) {
            throw new IllegalArgumentException(String.format(
                    "Given navigationTarget '%s' does not implement HasUrlParameter.",
                    navigationTarget.getName()));
        }

        Class<?> parameterType = getClassType(navigationTarget);

        Set<Class<?>> supportedTypes = ParameterDeserializer.supportedTypes;
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
    static Class<?> getClassType(Class<?> navigationTarget) {
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
    static boolean isAnnotatedParameter(Class<?> navigationTarget,
            Class<? extends Annotation> parameterAnnotation) {
        if (!HasUrlParameter.class.isAssignableFrom(navigationTarget)) {
            return false;
        }
        try {
            String methodName = "setParameter";
            assert methodName.equals(ReflectTools
                    .getFunctionalMethod(HasUrlParameter.class).getName());

            // Raw method has no parameter annotations if compiled by Eclipse
            Type parameterType = GenericTypeReflector.getTypeParameter(
                    navigationTarget,
                    HasUrlParameter.class.getTypeParameters()[0]);
            Class<?> parameterClass = GenericTypeReflector.erase(parameterType);

            Method setParameter = navigationTarget.getMethod(methodName,
                    BeforeNavigationEvent.class, parameterClass);
            return setParameter.getParameters()[1]
                    .isAnnotationPresent(parameterAnnotation);
        } catch (NoSuchMethodException e) {
            String msg = String.format(
                    "Failed to find HasUrlParameter::setParameter method when checking for @%s",
                    parameterAnnotation.getSimpleName());
            throw new IllegalStateException(msg, e);
        }
    }
}
