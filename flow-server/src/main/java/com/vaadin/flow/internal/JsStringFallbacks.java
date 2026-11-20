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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.internal.BundledEventData;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionProxy;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsStringFallback;
import com.vaadin.flow.js.internal.JsWithExecutionContext;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinService;

/**
 * Matches JavaScript that is sent as a string against the
 * {@link JsStringFallback} interfaces of the application, so that the browser
 * runs a function from the bundle instead of compiling the string.
 * <p>
 * The build checks the marked interfaces with {@link #collectFallbacks} and
 * lists their names in {@link Constants#JS_STRING_FALLBACKS_FILE}. The list is
 * read the first time a service matches a string, which only happens in the
 * modes that report string JavaScript.
 * <p>
 * A string matches what is declared for it when the text is the same, and for a
 * call or a function also the number of parameters: the same identity that
 * {@link JsCall#functionId(String, int, boolean)},
 * {@link JsCall#snippetFunctionId(String, List, int, boolean)} and
 * {@link BundledEventData#getFunctionId(String)} give the function in the
 * bundle.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class JsStringFallbacks {

    private static final String RESOURCE = Constants.VAADIN_SERVLET_RESOURCES
            + Constants.JS_STRING_FALLBACKS_FILE;

    private static final Map<VaadinService, Fallbacks> fallbacks = Collections
            .synchronizedMap(new WeakHashMap<>());

    /**
     * What the {@link JsStringFallback} interfaces of an application declare.
     *
     * @param calls
     *            the methods that run JavaScript, by function identifier
     * @param functions
     *            the methods that return a {@link JsFunction}, by function
     *            identifier
     * @param filters
     *            the declared filters
     * @param eventData
     *            the declared event data values
     */
    public record Fallbacks(Map<String, Method> calls,
            Map<String, Method> functions, Set<String> filters,
            Set<String> eventData) {

        private static final Fallbacks NONE = new Fallbacks(Map.of(), Map.of(),
                Set.of(), Set.of());
    }

    private JsStringFallbacks() {
    }

    /**
     * Checks the fallback interfaces and collects what they declare.
     *
     * @param definitionTypes
     *            the interfaces marked with {@link JsStringFallback}, not
     *            <code>null</code>
     * @return what the interfaces declare, not <code>null</code>
     * @throws IllegalArgumentException
     *             if a type is not a {@link JsDefinition}, declares something
     *             that no string can match, or two methods declare the same
     *             JavaScript with the same parameter count
     */
    public static Fallbacks collectFallbacks(
            Collection<Class<?>> definitionTypes) {
        Map<String, Method> calls = new HashMap<>();
        Map<String, Method> functions = new HashMap<>();
        Set<String> filters = new HashSet<>();
        Set<String> eventData = new HashSet<>();
        for (Class<?> definitionType : definitionTypes) {
            boolean snippets = checkFallbackDefinition(definitionType);
            Map<String, Method> byFunctionId = snippets ? functions : calls;
            for (Method method : collectDeclaredMethods(definitionType)) {
                // A string is found by what it sends: its text and its argument
                // or capture count. The names of a function are only known
                // later, from withArguments, so they are not part of it. The
                // function the bundle runs for a call has an identifier of its
                // own, see isStringFallbackCall
                String expression = method.getAnnotation(JsExpression.class)
                        .value();
                String key = snippets
                        ? JsCall.snippetFunctionId(expression, List.of(),
                                method.getParameterCount(), false)
                        : JsCall.functionId(expression,
                                method.getParameterCount(), method.isVarArgs());
                Method previous = byFunctionId.putIfAbsent(key, method);
                if (previous != null && !previous.equals(method)) {
                    throw new IllegalArgumentException("Methods "
                            + describe(previous) + " and " + describe(method)
                            + " declare the same JavaScript with the same"
                            + " parameter count, so a string could not tell"
                            + " which of them it stands for. Keep one of them");
                }
            }
            JsStringFallback annotation = definitionType
                    .getAnnotation(JsStringFallback.class);
            if (annotation != null) {
                filters.addAll(Arrays.asList(annotation.filters()));
                eventData.addAll(Arrays.asList(annotation.eventData()));
            }
        }
        return new Fallbacks(Map.copyOf(calls), Map.copyOf(functions),
                Set.copyOf(filters), Set.copyOf(eventData));
    }

    /**
     * Finds the declared call that stands in for an <code>executeJs</code> call
     * with a string expression, in the service of the current request.
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
        Fallbacks current = getFallbacks();
        Method method = (current == null) ? null
                : current.calls().get(JsCall.functionId(expression,
                        parameters.length, false));
        return (method == null) ? null
                : new JsCall(method.getDeclaringClass(), method.getName(),
                        Arrays.asList(parameters));
    }

    /**
     * Finds the declared call of a method returning a {@link JsFunction} that
     * stands in for a function created from a string body, in the service of
     * the current request.
     *
     * @param body
     *            the body of the function, not <code>null</code>
     * @param captures
     *            the values the function captures, not <code>null</code>
     * @return the call to create the declared function with, or
     *         <code>null</code> if the application declared nothing for the
     *         body, or the mode does not report string JavaScript
     * @throws IllegalStateException
     *             if the list the build wrote names a class that can not be
     *             loaded or no longer fits
     */
    public static @Nullable JsCall findDeclaredFunction(String body,
            @Nullable Object[] captures) {
        Fallbacks current = getFallbacks();
        // Found without names, which withArguments adds after this
        Method method = (current == null) ? null
                : current.functions().get(JsCall.snippetFunctionId(body,
                        List.of(), captures.length, false));
        return (method == null) ? null
                : new JsCall(method.getDeclaringClass(), method.getName(),
                        Arrays.asList(captures));
    }

    /**
     * Checks whether the application declared a filter, so that the bundle has
     * a function for it, in the service of the current request.
     *
     * @param filter
     *            the filter, not <code>null</code>
     * @return <code>true</code> if the filter is declared and the mode reports
     *         string JavaScript
     */
    public static boolean isDeclaredFilter(String filter) {
        Fallbacks current = getFallbacks();
        return current != null && current.filters().contains(filter);
    }

    /**
     * Checks whether the application declared an event data value, so that the
     * bundle has a function for it, in the service of the current request.
     *
     * @param eventData
     *            the event data value, possibly mapped to an element, not
     *            <code>null</code>
     * @return <code>true</code> if the value is declared and the mode reports
     *         string JavaScript
     */
    public static boolean isDeclaredEventData(String eventData) {
        Fallbacks current = getFallbacks();
        // The bundle has one function for a value, mapped to an element or not
        return current != null && current.eventData()
                .contains(BundledEventData.getExpression(eventData));
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
     * What the current service matches strings against, or <code>null</code>
     * when the mode does not report string JavaScript and strings are sent as
     * they are.
     */
    private static @Nullable Fallbacks getFallbacks() {
        if (!CspViolationReporter.isEnabled()) {
            return null;
        }
        // Enabled means there is a service with a configuration
        return fallbacks.computeIfAbsent(VaadinService.getCurrent(),
                JsStringFallbacks::loadFallbacks);
    }

    /**
     * Reads the interfaces the build listed for the application of a service.
     */
    private static Fallbacks loadFallbacks(VaadinService service) {
        ClassLoader classLoader = service.getClassLoader();
        List<Class<?>> definitionTypes = new ArrayList<>();
        try (InputStream list = classLoader.getResourceAsStream(RESOURCE)) {
            if (list == null) {
                return Fallbacks.NONE;
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

    /**
     * Checks that what a type declares can stand in for strings.
     *
     * @return whether the type declares snippets rather than calls
     */
    private static boolean checkFallbackDefinition(Class<?> definitionType) {
        boolean snippets = JsDefinitionProxy.checkDefinition(
                definitionType) == JsDefinitionProxy.Kind.SNIPPETS;
        // A matched string is sent as a call of the method found by its name
        // and parameter count, so two methods that share both could not be
        // told apart when the call is sent
        Map<String, Method> byNameAndCount = new HashMap<>();
        for (Method method : collectDeclaredMethods(definitionType)) {
            Method previous = byNameAndCount.putIfAbsent(
                    method.getName() + "/" + method.getParameterCount(),
                    method);
            if (previous != null) {
                throw new IllegalArgumentException("Methods "
                        + describe(previous) + " and " + describe(method)
                        + " have the same name and parameter count, so the"
                        + " call that stands in for a string could not tell"
                        + " them apart. Rename one of them");
            }
        }
        for (Method method : collectDeclaredMethods(definitionType)) {
            String name = "Method " + method.getName() + " of "
                    + definitionType.getName();
            // A string passes a fixed list of values: matching a rest
            // parameter would need the count of fixed parameters guessed
            if (method.isVarArgs()) {
                throw new IllegalArgumentException(
                        name + " is variadic. Declare one method per parameter"
                                + " count that the string is sent with");
            }
            if (method.isAnnotationPresent(JsWithExecutionContext.class)) {
                throw new IllegalArgumentException(name
                        + " gets the execution context, which a string call"
                        + " does not have");
            }
            if (snippets && !JsFunction.class.equals(method.getReturnType())) {
                throw new IllegalArgumentException(name + " returns "
                        + method.getReturnType().getSimpleName()
                        + ". Only a JsFunction can stand in for a function"
                        + " made from a string; list filters and event data"
                        + " in @JsStringFallback instead");
            }
        }
        JsStringFallback annotation = definitionType
                .getAnnotation(JsStringFallback.class);
        if (annotation != null) {
            for (String filter : annotation.filters()) {
                if (!BundledEventData.isBundledFilter(filter)) {
                    throw new IllegalArgumentException("The filter \"" + filter
                            + "\" declared by " + definitionType.getName()
                            + " is a property path, which the browser reads"
                            + " without compiling it. Remove it");
                }
            }
            for (String value : annotation.eventData()) {
                if (!ElementListenerMap.isCompiledByClient(value)) {
                    throw new IllegalArgumentException("The event data \""
                            + value + "\" declared by "
                            + definitionType.getName()
                            + " is a property path, which the browser reads"
                            + " without compiling it. Remove it");
                }
            }
        }
        return snippets;
    }

    private static List<Method> collectDeclaredMethods(
            Class<?> definitionType) {
        return Arrays.stream(definitionType.getMethods()).filter(
                method -> method.isAnnotationPresent(JsExpression.class))
                .toList();
    }

    /**
     * Checks whether the given method runs JavaScript in place of an
     * <code>executeJs</code> string, which is the case for a method that does
     * not return a snippet and is declared by a {@link JsStringFallback}
     * interface.
     * <p>
     * Such a method is generated so that its JavaScript sees what the string
     * would see: <code>Element.executeJs(String, Object...)</code> gives the
     * element to the string as one more parameter after the arguments, so a
     * string can use <code>$0</code> as the element when it has no arguments.
     * Since it is generated differently, its function has an identifier of its
     * own, so it never replaces the function of another definition that
     * declares the same JavaScript.
     *
     * @param method
     *            a method annotated with {@link JsExpression}, not
     *            <code>null</code>
     * @return <code>true</code> if the method stands in for a string call
     */
    public static boolean isStringFallbackCall(Method method) {
        return method.getDeclaringClass()
                .isAnnotationPresent(JsStringFallback.class)
                && !JsSnippet.class.isAssignableFrom(method.getReturnType());
    }

    /**
     * Gets the body of the generated function for a method that
     * {@link #isStringFallbackCall(Method) stands in for a string call}. It
     * runs the declared JavaScript the way
     * <code>Element.executeJs(String, Object...)</code> runs the string: in a
     * function whose <code>this</code> is the element and which sees the
     * element as the parameter after the arguments.
     *
     * @param expression
     *            the declared JavaScript, not <code>null</code>
     * @param parameterCount
     *            the number of parameters of the method
     * @return the lines of the function body, not <code>null</code>
     */
    public static List<String> stringFallbackBody(String expression,
            int parameterCount) {
        return List.of("  return (async function ($" + parameterCount + ") {",
                expression, "  }).call(this, this);");
    }

    private static String describe(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }
}
