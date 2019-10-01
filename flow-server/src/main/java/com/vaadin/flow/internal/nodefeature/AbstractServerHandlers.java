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
package com.vaadin.flow.internal.nodefeature;

import java.lang.annotation.Annotation;
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

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;

/**
 * Abstract class for collecting Methods which are published as
 * <code>serverObject.&lt;name&gt;</code> on the client side.
 *
 * @param <T>
 *            Component type for setComponent(T component)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractServerHandlers<T>
        extends SerializableNodeList<String> {

    private Map<String, DisabledUpdateMode> disabledRpcModes;

    /**
     * Creates a new meta information list for the given state node.
     *
     * @param node
     *            the state node this list belongs to
     */
    public AbstractServerHandlers(StateNode node) {
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
        collectHandlerMethods(component.getClass());
    }

    /**
     * Gets RPC control mode from the client side to the server side for
     * disabled element.
     *
     * @param handler
     *            the handler name to get control mode
     * @return the handler RPC control mode for disabled element
     */
    public DisabledUpdateMode getDisabledUpdateMode(String handler) {
        DisabledUpdateMode mode = disabledRpcModes == null ? null
                : disabledRpcModes.get(handler);
        if (mode == null) {
            return DisabledUpdateMode.ONLY_WHEN_ENABLED;
        }
        return mode;
    }

    /**
     * Checks whether the handler is registered in this feature.
     *
     * @param handler
     *            the handler to check
     * @return {@code true} if handler is registered in the feature
     */
    public boolean hasHandler(String handler) {
        return indexOf(handler) != -1;
    }

    /**
     * Collect methods annotated with the handler annotation for given class.
     *
     * @param classWithAnnotations
     *            Class to collect methods for
     */
    protected void collectHandlerMethods(Class<?> classWithAnnotations) {
        List<Method> methods = new ArrayList<>();
        collectHandlerMethods(classWithAnnotations, methods);
        Map<String, Method> map = new HashMap<>();
        for (Method method : methods) {
            Method existing = map.get(method.getName());
            if (existing != null && !Arrays.equals(existing.getParameterTypes(),
                    method.getParameterTypes())) {
                String msg = String.format(Locale.ENGLISH,
                        "There may be only one handler method with the given name. "
                                + "Class '%s' (considering its superclasses) "
                                + "contains several handler methods with the same name: '%s'",
                        classWithAnnotations.getName(), method.getName());
                throw new IllegalStateException(msg);
            }
            map.put(method.getName(), method);
        }
        map.values().forEach(
                method -> add(method.getName(), getUpdateMode(method)));
    }

    /**
     * Collect all Methods annotated with the handler annotation.
     *
     * @param clazz
     *            Class to check methods for
     * @param methods
     *            Collection to add methods to
     */
    protected void collectHandlerMethods(Class<?> clazz,
            Collection<Method> methods) {
        if (clazz.equals(getType())) {
            return;
        }
        Stream.of(clazz.getDeclaredMethods()).filter(
                method -> method.isAnnotationPresent(getHandlerAnnotation()))
                .forEach(method -> addHandlerMethod(method, methods));
        collectHandlerMethods(clazz.getSuperclass(), methods);
    }

    /**
     * Add a handler to the NodeList.
     *
     * @param method
     *            Method to verify and add
     * @param methods
     *            Collection to add method to
     */
    protected void addHandlerMethod(Method method, Collection<Method> methods) {
        ensureSupportedParameterTypes(method);
        ensureSupportedReturnType(method);
        Optional<Class<?>> checkedException = Stream
                .of(method.getExceptionTypes())
                .filter(ReflectTools::isCheckedException).findFirst();
        if (checkedException.isPresent()) {
            String msg = String.format(Locale.ENGLISH,
                    "Handler method may not declare checked exceptions. "
                            + "Component '%s' has method '%s' which declares checked exception '%s'"
                            + " and annotated with '%s'",
                    method.getDeclaringClass().getName(), method.getName(),
                    checkedException.get().getName(),
                    getHandlerAnnotation().getName());
            throw new IllegalStateException(msg);
        }
        methods.add(method);
    }

    /**
     * Validate return type support for given method.
     *
     * @param method
     *            method to check return type for
     */
    protected void ensureSupportedReturnType(Method method) {
        if (!void.class.equals(method.getReturnType())) {
            String msg = String.format(Locale.ENGLISH,
                    "Only void handler methods are supported. "
                            + "Component '%s' has method '%s' annotated with '%s' whose return type is not void but \"%s\"",
                    method.getDeclaringClass().getName(), method.getName(),
                    getHandlerAnnotation().getName(),
                    method.getReturnType().getSimpleName());
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Gets the annotation which is used to mark methods as handlers.
     *
     * @return the handler marker annotation
     */
    protected abstract Class<? extends Annotation> getHandlerAnnotation();

    /**
     * Returns method's RPC communication mode from the client side to the
     * server side when the element is disabled.
     *
     * @param method
     *            the method to get its update mode
     * @return RPC communication mode for the method, not {@code null}
     */
    protected abstract DisabledUpdateMode getUpdateMode(Method method);

    private void add(String handler, DisabledUpdateMode mode) {
        add(handler);
        if (!DisabledUpdateMode.ONLY_WHEN_ENABLED.equals(mode)) {
            if (disabledRpcModes == null) {
                disabledRpcModes = new HashMap<>();
            }
            disabledRpcModes.put(handler, mode);
        }
    }

    @SuppressWarnings("unchecked")
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
            return "AbstractServerHandlers is used as raw type: either add type information or override collectHandlerMethods(Class<?> clazz, Collection<Method> methods).";
        }

        if (type instanceof TypeVariable) {
            return String.format(
                    "Could not determine the composite content type for TypeVariable '%s'. "
                            + "Either specify exact type or override collectHandlerMethods().",
                    type.getTypeName());
        }
        return String.format(
                "Could not determine the composite content type for %s. Override collectHandlerMethods().",
                type.getTypeName());
    }
}
