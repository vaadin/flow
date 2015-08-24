/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.ui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.JsonConverter;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.parser.EventBinding;
import com.vaadin.hummingbird.parser.TemplateParser;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

public abstract class Template extends AbstractComponent {
    private final StateNode node = StateNode.create();

    public Template() {
        this(null);
    }

    protected Template(ElementTemplate elementTemplate) {
        if (elementTemplate == null) {
            elementTemplate = TemplateParser.parse(getClass());
        }
        setElement(Element.getElement(elementTemplate, node));

        getNode().put(TemplateEventHandler.class, this::handleTemplateEvent);
    }

    private void handleTemplateEvent(StateNode node, ElementTemplate template,
            String eventType, JsonObject eventData) {
        Element element = Element.getElement(template, node);
        List<EventBinding> eventBindings = ((BoundElementTemplate) template)
                .getEventBindings(eventType);
        for (EventBinding eventBinding : eventBindings) {
            String methodName = eventBinding.getMethodName();
            List<String> paramsDefinitions = eventBinding.getParams();

            Object[] params = new Object[paramsDefinitions.size()];
            for (int i = 0; i < params.length; i++) {
                String definition = paramsDefinitions.get(i);
                if ("element".equals(definition)) {
                    params[i] = element;
                } else {
                    params[i] = eventData.get(definition);
                }
            }

            onBrowserEvent(node, element, methodName, params);
        }
    }

    protected void onBrowserEvent(StateNode node, Element element,
            String methodName, Object[] params) {
        Method method = findTemplateEventHandlerMethod(getClass(), methodName);
        if (method == null) {
            throw new RuntimeException("Couldn't find any @"
                    + com.vaadin.annotations.TemplateEventHandler.class
                            .getName()
                    + " method named " + methodName + " in "
                    + getClass().getName());
        }

        int paramIndex = 0;

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] methodParams = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (StateNode.class.isAssignableFrom(type)) {
                methodParams[i] = node;
            } else {
                Object param = params[paramIndex++];
                if (param == null || type.isInstance(param)) {
                    methodParams[i] = param;
                } else if (param instanceof JsonValue) {
                    methodParams[i] = JsonConverter.fromJson(parameterTypes[i],
                            (JsonValue) param);
                } else {
                    throw new RuntimeException(
                            "Can't convert " + param.getClass().getName()
                                    + " to " + type.getName());
                }
            }
        }

        method.setAccessible(true);
        try {
            method.invoke(this, methodParams);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new RuntimeException("Couldn't invoke " + method, e);
        }
    }

    private static Method findTemplateEventHandlerMethod(Class<?> type,
            String methodName) {
        while (type != null && type != Template.class) {
            Method[] declaredMethods = type.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.getName().equals(methodName) && method.getAnnotation(
                        com.vaadin.annotations.TemplateEventHandler.class) != null) {
                    return method;
                }
            }

            type = type.getSuperclass();
        }

        return null;
    }

    protected StateNode getNode() {
        return node;
    }

}
