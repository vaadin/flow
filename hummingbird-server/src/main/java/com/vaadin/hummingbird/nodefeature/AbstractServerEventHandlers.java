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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.util.ReflectTools;

/**
 * Abstract class for collecting Methods which are published as
 * <code>serverObject.&lt;name&gt;</code> on the client side.
 *
 * @param <T>
 *            Component type for setComponent(T component)
 *
 * @author Vaadin Ltd
 */
public abstract class AbstractServerEventHandlers<T>
        extends SerializableNodeList<String> {

    /**
     * Creates a new meta information list for the given state node.
     *
     * @param node
     *            the state node this list belongs to
     */
    public AbstractServerEventHandlers(StateNode node) {
        super(node);
    }

    /**
     * Validate parameter support for given method. Should validate parameter
     * amount and parameter types.
     *
     * @param method
     *            Method to check parameters for
     */
    protected abstract void ensureSupportedParameterTypes(Method method);

    /**
     * Called by {@link ComponentMapping} whenever a component instance has been
     * set for the node.
     *
     * @param component
     *            the component instance which was set
     */
    public void componentSet(T component) {
        assert component != null;
        collectEventHandlerMethods(component.getClass());
    }

    /**
     * Collect methods annotated with {@link EventHandler} for given class.
     * 
     * @param classWithAnnotations
     *            Class to collect methods for
     */
    protected void collectEventHandlerMethods(Class<?> classWithAnnotations) {
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
        map.keySet().forEach(this::add);
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
        if (clazz.equals(getType())) {
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

    private final Class<T> getType() {
        Type type = GenericTypeReflector.getTypeParameter(
                getClass().getGenericSuperclass(),
                getClass().getSuperclass().getTypeParameters()[0]);
        if (type instanceof Class || type instanceof ParameterizedType) {
            return (Class<T>) GenericTypeReflector.erase(type);
        }
        throw new IllegalStateException(getExceptionMessage(type));
    }

    private static String getExceptionMessage(Type type) {
        if (type == null) {
            return "AbstractServerEventHandlers is used as raw type: either add type information or override collectEventHandlerMethods(Class<?> clazz, Collection<Method> methods).";
        }

        if (type instanceof TypeVariable) {
            return String.format(
                    "Could not determine the composite content type for TypeVariable '%s'. "
                            + "Either specify exact type or override collectEventHandlerMethods().",
                    type.getTypeName());
        }
        return String.format(
                "Could not determine the composite content type for %s. Override collectEventHandlerMethods().",
                type.getTypeName());
    }
}
