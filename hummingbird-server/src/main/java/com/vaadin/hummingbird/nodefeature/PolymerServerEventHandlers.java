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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.annotations.EventData;
import com.vaadin.annotations.EventHandler;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.ui.Component;
import com.vaadin.util.ReflectTools;

/**
 * Methods which are published as event-handlers on the client side.
 *
 * @author Vaadin Ltd
 *
 */
public class PolymerServerEventHandlers extends PublishedServerEventHandlers {

    /**
     * Creates a new meta information list for the given state node.
     *
     * @param node
     *            the state node this list belongs to
     */
    public PolymerServerEventHandlers(StateNode node) {
        super(node);
    }

    /**
     * Called by {@link ComponentMapping} whenever a component instance has been
     * set for the node.
     *
     * @param component
     *            the component instance which was set
     */
    @Override
    public void componentSet(Component component) {
        assert component != null;
        assert component instanceof PolymerTemplate;
        collectEventHandlerMethods(component.getClass());
    }

    @Override
    protected void collectEventHandlerMethods(Class<?> clazz,
            Collection<Method> methods) {
        if (clazz.equals(PolymerTemplate.class)) {
            return;
        }
        Stream.of(clazz.getDeclaredMethods()).filter(
                method -> method.isAnnotationPresent(EventHandler.class))
                .forEach(method -> {
                    addPolymerEventHandlerMethod(method, methods);
                    String[] parameters = getParameters(method);
                    if (parameters.length > 0)
                        getNode().getFeature(PolymerEventListenerMap.class)
                                .add(method.getName(), parameters);
                });
        collectEventHandlerMethods(clazz.getSuperclass(), methods);
    }

    private String[] getParameters(Method method) {
        Parameter[] parameters = method.getParameters();
        List<String> params = new LinkedList<>();

        Stream.of(parameters).forEach(parameter -> params
                .add(parameter.getAnnotation(EventData.class).value()));
        return params.toArray(new String[params.size()]);
    }

    private void addPolymerEventHandlerMethod(Method method,
            Collection<Method> methods) {
        ensureSupportedParameterTypes(method);
        if (!void.class.equals(method.getReturnType())) {
            String msg = String.format(Locale.ENGLISH,
                    "Only void event handler methods are supported. "
                            + "Template component '%s' has method '%s' "
                            + "annotated with '%s' whose return type is not void but %s",
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
                            + "Template component '%s' has method '%s' which "
                            + "declares checked exception '%s' and annotated with '%s'",
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
        Stream.of(method.getParameters())
                .forEach(parameter -> ensureAnnotation(method, parameter));
    }

    private static void ensureAnnotation(Method method, Parameter parameter) {
        if (!parameter.isAnnotationPresent(EventData.class)) {
            throw new IllegalStateException(
                    "No @EventData annotation on parameter "
                            + parameter.getName().replace("arg", "")
                            + " for EventHandler method" + method.getName());
        }
    }

}
