/*
 * Copyright 2000-2016 Vaadin Ltd.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.ui.Component;
import com.vaadin.util.ReflectTools;

/**
 * Template meta data information: list of template methods annotated with @
 * {@link EventHandler}.
 *
 * @author Vaadin Ltd
 *
 */
public class TemplateEventHandlerNames extends SerializableNodeList<String> {

    private static final String ERROR_MSG = "The "
            + TemplateEventHandlerNames.class.getSimpleName()
            + " may be used only for components";
    private boolean isInfoCollected;

    /**
     * Creates a new meta information list for the given template node.
     *
     * @param node
     *            the template node that the list belongs to
     */
    public TemplateEventHandlerNames(StateNode node) {
        super(node);
    }

    @Override
    public void onAttach(boolean initialAttach) {
        if (!isInfoCollected) {
            collectEventHandlerMethods(getComponent());
        }
        isInfoCollected = true;
    }

    private void collectEventHandlerMethods(Component component) {
        List<Method> methods = new ArrayList<>();
        collectEventHandlerMethods(component.getClass(), methods);
        Map<String, Method> map = new HashMap<>();
        for (Method method : methods) {
            Method existing = map.get(method.getName());
            if (existing != null && !Arrays.asList(existing.getParameterTypes())
                    .equals(Arrays.asList(method.getParameterTypes()))) {
                StringBuilder builder = new StringBuilder(
                        "There may be only one event handler method with the given name. Class ");
                builder.append(component.getClass());
                builder.append(
                        " (considering with its superclasses) contains several event handler methods with the same name:");
                builder.append(method.getName());
                throw new IllegalStateException(builder.toString());
            }
            map.put(method.getName(), method);
        }
        map.keySet().forEach(this::add);
    }

    private void collectEventHandlerMethods(Class<?> clazz,
            Collection<Method> methods) {
        if (clazz.equals(Component.class)) {
            return;
        }
        Stream.of(clazz.getDeclaredMethods())
                .filter(method -> method
                        .isAnnotationPresent(EventHandler.class))
                .forEach(method -> addEventHandlerMethod(method, methods));
        collectEventHandlerMethods(clazz.getSuperclass(), methods);
    }

    private Component getComponent() {
        assert getNode().hasFeature(ComponentMapping.class) : ERROR_MSG;
        Optional<Component> component = getNode()
                .getFeature(ComponentMapping.class).getComponent();
        assert component.isPresent() : ERROR_MSG;
        return component.get();
    }

    private void addEventHandlerMethod(Method method,
            Collection<Method> methods) {
        checkParameterTypes(method);
        if (!void.class.equals(method.getReturnType())) {
            StringBuilder builder = new StringBuilder(
                    "Non void event handler methods (no return type) are not supported. Component ");
            builder.append(method.getDeclaringClass());
            builder.append(" has method ").append(method.getName());
            builder.append(" whose return type is not void annotated with ");
            builder.append(EventHandler.class);
            throw new IllegalStateException(builder.toString());
        }
        Optional<Class<?>> checkedException = Stream
                .of(method.getExceptionTypes()).filter(this::isCheckedException)
                .findFirst();
        if (checkedException.isPresent()) {
            StringBuilder builder = new StringBuilder(
                    "Event handler method may not declare checked exceptions. Component ");
            builder.append(method.getDeclaringClass());
            builder.append(" has method ").append(method.getName());
            builder.append(" which declares checked exception ");
            builder.append(checkedException.get());
            builder.append(" and annotated with ");
            builder.append(EventHandler.class);
            throw new IllegalStateException(builder.toString());
        }
        methods.add(method);

    }

    private static void checkParameterTypes(Method method) {
        if (method.getParameterCount() == 0) {
            return;
        }
        Stream.of(method.getParameterTypes())
                .forEach(type -> checkParameterType(method, type));
    }

    private static void checkParameterType(Method method, Class<?> type) {
        Class<?> parameterType = ReflectTools.convertPrimitiveType(type);
        if (parameterType.isArray()) {
            checkParameterType(method, parameterType.getComponentType());
        } else if (!JsonCodec.canEncodeWithoutTypeInfo(parameterType)) {
            StringBuilder builder = new StringBuilder(
                    "Event handler method may contain only serializable to JSON types. Component ");
            builder.append(method.getDeclaringClass());
            builder.append(" has method ").append(method.getName());
            builder.append(
                    " which declares parameter with non serializable to JSON type");
            builder.append(parameterType);
            builder.append(" and annotated with ");
            builder.append(EventHandler.class);
            throw new IllegalStateException(builder.toString());
        }
    }

    private boolean isCheckedException(Class<?> exceptionClass) {
        return !RuntimeException.class.isAssignableFrom(exceptionClass)
                && !Error.class.isAssignableFrom(exceptionClass);
    }

}
