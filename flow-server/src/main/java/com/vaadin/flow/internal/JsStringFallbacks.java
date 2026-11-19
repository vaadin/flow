/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionProxy;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsStringFallback;
import com.vaadin.flow.js.internal.JsWithExecutionContext;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinService;

/**
 * Matches JavaScript that is sent as a string against the
 * {@link JsStringFallback} interfaces of the application, so that the call runs
 * the declared function from the bundle instead of compiling the string in the
 * browser.
 * <p>
 * The build checks the marked interfaces with {@link #collectFallbacks} and
 * lists their names in {@link Constants#JS_STRING_FALLBACKS_FILE}. The list is
 * read the first time a service matches a call, which only happens in the modes
 * that report string JavaScript.
 * <p>
 * A string call matches a declared method when the text and the parameter count
 * are the same, which is the identity
 * {@link JsCall#functionId(String, int, boolean)} gives the function in the
 * bundle.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class JsStringFallbacks {

    private static final String RESOURCE = Constants.VAADIN_SERVLET_RESOURCES
            + Constants.JS_STRING_FALLBACKS_FILE;

    private static final Map<VaadinService, Map<String, Method>> fallbacks = Collections
            .synchronizedMap(new WeakHashMap<>());

    private JsStringFallbacks() {
    }

    /**
     * Checks the fallback interfaces and maps each declared method by the
     * identifier of its function.
     *
     * @param definitionTypes
     *            the interfaces marked with {@link JsStringFallback}, not
     *            <code>null</code>
     * @return the declared methods by function identifier, not
     *         <code>null</code>
     * @throws IllegalArgumentException
     *             if a type is not a {@link JsDefinition} of calls, has a
     *             method that a string call can not match, or two methods
     *             declare the same JavaScript with the same parameter count
     */
    public static Map<String, Method> collectFallbacks(
            Collection<Class<?>> definitionTypes) {
        Map<String, Method> byFunctionId = new HashMap<>();
        for (Class<?> definitionType : definitionTypes) {
            checkFallbackDefinition(definitionType);
            for (Method method : collectDeclaredMethods(definitionType)) {
                Method previous = byFunctionId
                        .putIfAbsent(JsCall.functionId(method), method);
                if (previous != null && !previous.equals(method)) {
                    throw new IllegalArgumentException("Methods "
                            + describe(previous) + " and " + describe(method)
                            + " declare the same JavaScript with the same"
                            + " parameter count, so a string call could not"
                            + " tell which of them it stands for. Keep one of"
                            + " them");
                }
            }
        }
        return Map.copyOf(byFunctionId);
    }

    /**
     * Finds the declared call that stands in for JavaScript sent as a string,
     * in the service of the current request.
     *
     * @param expression
     *            the JavaScript the string call sends, not <code>null</code>
     * @param parameters
     *            the parameters of the string call, not <code>null</code>
     * @return the declared call to send instead, or <code>null</code> if the
     *         application declared nothing for the string, or the mode does not
     *         report string JavaScript
     * @throws IllegalStateException
     *             if the list the build wrote names a class that can not be
     *             loaded or no longer fits
     */
    public static @Nullable JsCall findDeclaredCall(String expression,
            @Nullable Object[] parameters) {
        if (!CspViolationReporter.isEnabled()) {
            return null;
        }
        // Enabled means there is a service with a configuration
        VaadinService service = VaadinService.getCurrent();
        Method method = fallbacks
                .computeIfAbsent(service, JsStringFallbacks::loadFallbacks)
                .get(JsCall.functionId(expression, parameters.length, false));
        if (method == null) {
            return null;
        }
        return new JsCall(method.getDeclaringClass(), method.getName(),
                Arrays.asList(parameters));
    }

    /**
     * Reads the list again for a service, after a class change updated it while
     * the application runs. A list that does not fit is logged rather than
     * thrown, since no call is being made, and the next call that is matched
     * reports it again.
     *
     * @param service
     *            the service, not <code>null</code>
     */
    public static void reloadFallbacks(VaadinService service) {
        fallbacks.remove(service);
        try {
            fallbacks.put(service, loadFallbacks(service));
        } catch (RuntimeException e) {
            LoggerFactory.getLogger(JsStringFallbacks.class).warn(
                    "The changed @JsStringFallback interfaces can not stand in"
                            + " for JavaScript sent as a string, so calls that"
                            + " would match them fail until this is fixed",
                    e);
        }
    }

    /**
     * Reads the interfaces the build listed for the application of a service.
     */
    private static Map<String, Method> loadFallbacks(VaadinService service) {
        ClassLoader classLoader = service.getClassLoader();
        List<Class<?>> definitionTypes = new ArrayList<>();
        try (InputStream list = classLoader.getResourceAsStream(RESOURCE)) {
            if (list == null) {
                return Map.of();
            }
            JsonNode names = JacksonUtils.getMapper().readTree(list);
            for (JsonNode name : names) {
                definitionTypes.add(
                        Class.forName(name.asString(), false, classLoader));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + RESOURCE, e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(RESOURCE
                    + " names a class that is not on the classpath: "
                    + e.getMessage()
                    + ". The build that wrote it saw other classes than the"
                    + " application has", e);
        }
        try {
            return collectFallbacks(definitionTypes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("An interface listed in " + RESOURCE
                    + " no longer fits what the build checked", e);
        }
    }

    private static void checkFallbackDefinition(Class<?> definitionType) {
        if (JsDefinitionProxy.checkDefinition(
                definitionType) != JsDefinitionProxy.Kind.CALLS) {
            throw new IllegalArgumentException(definitionType.getName()
                    + " declares snippets, but only a call can stand in for"
                    + " JavaScript that is sent as a string");
        }
        for (Method method : collectDeclaredMethods(definitionType)) {
            // A string call passes a fixed list of values: matching a rest
            // parameter would need the count of fixed parameters guessed
            if (method.isVarArgs()) {
                throw new IllegalArgumentException("Method " + method.getName()
                        + " of " + definitionType.getName()
                        + " is variadic. Declare one method per parameter"
                        + " count that the string call is made with");
            }
            if (method.isAnnotationPresent(JsWithExecutionContext.class)) {
                throw new IllegalArgumentException("Method " + method.getName()
                        + " of " + definitionType.getName()
                        + " gets the execution context, which a string call"
                        + " does not have");
            }
        }
    }

    private static List<Method> collectDeclaredMethods(
            Class<?> definitionType) {
        return Arrays.stream(definitionType.getMethods()).filter(
                method -> method.isAnnotationPresent(JsExpression.class))
                .toList();
    }

    private static String describe(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }
}
