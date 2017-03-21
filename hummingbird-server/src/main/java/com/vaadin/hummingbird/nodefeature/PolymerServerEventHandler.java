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
package com.vaadin.hummingbird.nodefeature;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.annotations.EventData;
import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.server.communication.MethodInvocationUtil;
import com.vaadin.util.ReflectTools;

/**
 * Methods which are published as event-handlers on the client side.
 *
 * @author Vaadin Ltd
 *
 */
public class PolymerServerEventHandler {

    private PolymerServerEventHandler() {
    }

    /**
     * Get instance of PolymerServerEventHandler.
     * 
     * @return
     */
    public static PolymerServerEventHandler instance() {
        return new PolymerServerEventHandler();
    }

    /**
     * Bind event handlers for given {@link PolymerTemplate} component.
     * 
     * @param component
     *            Component to bind event handlers for
     */
    public void bindComponentMethods(final PolymerTemplate<?> component) {
        Map<String, Method> map = collectEventHandlerMethods(
                component.getClass());

        map.keySet()
                .forEach(method -> component.getElement().addCallback(method,
                        data -> MethodInvocationUtil.invokeMethod(component,
                                component.getClass(), method, data),
                        getParameters(map.get(method))));
    }

    protected Map<String, Method> collectEventHandlerMethods(
            Class<?> classWithAnnotations) {
        List<Method> methods = new ArrayList<>();
        collectEventHandlerMethods(classWithAnnotations, methods);
        Map<String, Method> map = new HashMap<>();
        for (Method method : methods) {
            Method existing = map.get(method.getName());
            if (existing != null && !Arrays.equals(existing.getParameterTypes(),
                    method.getParameterTypes())) {
                String msg = String.format(Locale.ENGLISH,
                        "There may be only one event handler method with the given name. "
                                + "Class '%s' (considering its superclasses) "
                                + "contains several event handler methods with the same name: '%s'",
                        classWithAnnotations.getName(), method.getName());
                throw new IllegalStateException(msg);
            }
            map.put(method.getName(), method);
        }
        return map;
    }

    /**
     * Collect all Methods annotated with {@link EventHandler}.
     *
     * @param clazz
     *            Class to check methods for
     * @param methods
     *            Collection to add methods to
     */
    protected void collectEventHandlerMethods(Class<?> clazz,
            Collection<Method> methods) {
        if (clazz.equals(PolymerTemplate.class)) {
            return;
        }
        Stream.of(clazz.getDeclaredMethods()).filter(
                method -> method.isAnnotationPresent(EventHandler.class))
                .forEach(method -> addEventHandlerMethod(method, methods));
        collectEventHandlerMethods(clazz.getSuperclass(), methods);
    }

    /**
     * Add an event handler to the NodeList.
     *
     * @param method
     *            Method to verify and add
     * @param methods
     *            Collection to add method to
     */
    protected void addEventHandlerMethod(Method method,
            Collection<Method> methods) {
        ensureSupportedParameterTypes(method);
        if (!void.class.equals(method.getReturnType())) {
            String msg = String.format(Locale.ENGLISH,
                    "Only void event handler methods are supported. "
                            + "Component '%s' has method '%s' annotated with '%s' whose return type is not void but %s",
                    method.getDeclaringClass().getName(), method.getName(),
                    EventHandler.class.getName(),
                    method.getReturnType().getSimpleName());
            throw new IllegalStateException(msg);
        }
        Optional<Class<?>> checkedException = Stream
                .of(method.getExceptionTypes())
                .filter(ReflectTools::isCheckedException).findFirst();
        if (checkedException.isPresent()) {
            String msg = String.format(Locale.ENGLISH,
                    "Event handler method may not declare checked exceptions. "
                            + "Component '%s' has method '%s' which declares checked exception '%s'"
                            + " and annotated with '%s'",
                    method.getDeclaringClass().getName(), method.getName(),
                    checkedException.get().getName(),
                    EventHandler.class.getName());
            throw new IllegalStateException(msg);
        }
        methods.add(method);
    }

    /**
     * Ensure that method parameter type is supported e.g. parameter types of
     * event handler methods must be serializable to JSON.
     *
     * @param method
     *            Method we are checking
     * @param type
     *            The parameter type
     */
    protected static void ensureSupportedParameterType(Method method,
            Class<?> type) {
        Class<?> parameterType = ReflectTools.convertPrimitiveType(type);
        if (parameterType.isArray()) {
            ensureSupportedParameterType(method,
                    parameterType.getComponentType());
        } else if (!JsonCodec.canEncodeWithoutTypeInfo(parameterType)) {
            String msg = String.format(Locale.ENGLISH,
                    "The parameter types of event handler methods must be serializable to JSON."
                            + " Component %s has method '%s' and annotated with %s "
                            + "which declares parameter with non serializable to JSON type '%s'",
                    method.getDeclaringClass().getName(), method.getName(),
                    EventHandler.class.getName(), type.getName());
            throw new IllegalStateException(msg);
        }
    }

    protected void ensureSupportedParameterTypes(Method method) {
        if (method.getParameterCount() == 0) {
            return;
        }
        Stream.of(method.getParameterTypes())
                .forEach(type -> ensureSupportedParameterType(method, type));
        Stream.of(method.getParameters())
                .forEach(parameter -> ensureAnnotation(method, parameter));
    }

    private static void ensureAnnotation(Method method, Parameter parameter) {
        if (!parameter.isAnnotationPresent(EventData.class)) {
            String msg = String.format(
                    "No @EventData annotation on parameter '%s'"
                            + " for EventHandler method '%s'",
                    parameter.getName().replace("arg", ""), method.getName());
            throw new IllegalStateException(msg);
        }
    }

    private String[] getParameters(Method method) {
        Parameter[] parameters = method.getParameters();

        return Stream.of(parameters).map(
                parameter -> parameter.getAnnotation(EventData.class).value())
                .toArray(size -> new String[size]);
    }
}
