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
import java.util.stream.Stream;

import com.vaadin.annotations.EventData;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.template.PolymerTemplate;

/**
 * Methods which are published as event-handlers on the client side.
 *
 * @author Vaadin Ltd
 *
 */
public class PolymerServerEventHandlers
        extends AbstractServerEventHandlers<PolymerTemplate> {

    /**
     * Creates a new meta information list for the given state node.
     *
     * @param node
     *            the state node this list belongs to
     */
    public PolymerServerEventHandlers(StateNode node) {
        super(node);
    }

    @Override
    protected Class getType() {
        return PolymerTemplate.class;
    }

    @Override
    protected void addEventHandlerMethod(Method method,
            Collection<Method> methods) {
        super.addEventHandlerMethod(method, methods);

        addMethodParameters(method);
    }

    @Override
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

    private void addMethodParameters(Method method) {
        getNode().getFeature(PolymerEventListenerMap.class)
                .add(method.getName(), getParameters(method));
    }

    private String[] getParameters(Method method) {
        Parameter[] parameters = method.getParameters();

        return Stream.of(parameters).map(
                parameter -> parameter.getAnnotation(EventData.class).value())
                .toArray(size -> new String[size]);
    }

}
