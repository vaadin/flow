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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.router.event.BeforeNavigationEvent;
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
    default T deserializeUrlParameters(Class<?> navigationTarget,
            List<String> urlParameters) {
        if (urlParameters.isEmpty()) {
            return isAnnotatedParameter(navigationTarget,
                    WildcardParameter.class) ? (T) "" : null;
        }
        if (isAnnotatedParameter(navigationTarget, WildcardParameter.class)) {
            return (T) urlParameters.stream().collect(Collectors.joining("/"));
        }
        return (T) urlParameters.get(0);
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
        Type type = GenericTypeReflector.getTypeParameter(navigationTarget,
                HasUrlParameter.class.getTypeParameters()[0]);
        if (!(type instanceof Class<?>)) {
            throw new IllegalArgumentException(String.format(
                    "Parameter type of the given navigationTarget '%s' could not be resolved.",
                    navigationTarget.getName()));
        }
        Class<?> parameterType = GenericTypeReflector.erase(type);
        Set<Class<?>> supportedTypes = Stream
                .of(Long.class, Integer.class, String.class)
                .collect(Collectors.toSet());
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
     * Check if the parameter value is annotated as OptionalParameter.
     * 
     * @param navigationTarget
     *            navigation target to check for optional
     * @return parameter is optional
     */
    static boolean isAnnotatedParameter(Class<?> navigationTarget,
            Class parameterClass) {
        if (!HasUrlParameter.class.isAssignableFrom(navigationTarget)) {
            return false;
        }
        try {
            Method setParameter = navigationTarget.getMethod(
                    ReflectTools.getFunctionalMethod(HasUrlParameter.class)
                            .getName(),
                    BeforeNavigationEvent.class, Object.class);
            return setParameter.getParameters()[1]
                    .isAnnotationPresent(parameterClass);
        } catch (NoSuchMethodException e) {
            Logger.getLogger(HasUrlParameter.class.getName()).log(Level.WARNING,
                    "Failed to get setParameter method for checking for @Optional");
        }

        return false;
    }
}
