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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.JsonCodec;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.ui.Component;
import com.vaadin.util.ReflectTools;

/**
 * Methods which are published as <code>element.$server.&lt;name&gt;</code> on
 * the client side.
 *
 * @author Vaadin Ltd
 *
 */
public class PublishedServerEventHandlers extends SerializableNodeList<String> {

    /**
     * Creates a new meta information list for the given state node.
     *
     * @param node
     *            the state node this list belongs to
     */
    public PublishedServerEventHandlers(StateNode node) {
        super(node);
    }

    /**
     * Called by {@link ComponentMapping} whenever a component instance has been
     * set for the node.
     *
     * @param component
     *            the component instance which was set
     */
    public void componentSet(Component component) {
        assert component != null;
        collectEventHandlerMethods(component.getClass());
    }

    private void collectEventHandlerMethods(Class<?> classWithAnnotations) {
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

    private void collectEventHandlerMethods(Class<?> clazz,
            Collection<Method> methods) {
        if (clazz.equals(Component.class)) {
            return;
        }
        Stream.of(clazz.getDeclaredMethods()).filter(
                method -> method.isAnnotationPresent(EventHandler.class))
                .forEach(method -> addEventHandlerMethod(method, methods));
        collectEventHandlerMethods(clazz.getSuperclass(), methods);
    }

    private void addEventHandlerMethod(Method method,
            Collection<Method> methods) {
        ensureSupportedParameterTypes(method);
        if (!void.class.equals(method.getReturnType())) {
            String msg = String.format(Locale.ENGLISH,
                    "Non void event handler methods (no return type) are not supported. "
                            + "Component '%s' has method '%s' annotated with '%s' whose return type is not void",
                    method.getDeclaringClass().getName(), method.getName(),
                    EventHandler.class.getName());
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

    private static void ensureSupportedParameterTypes(Method method) {
        if (method.getParameterCount() == 0) {
            return;
        }
        Stream.of(method.getParameterTypes())
                .forEach(type -> ensureSupportedParameterType(method, type));
    }

    private static void ensureSupportedParameterType(Method method,
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

}
