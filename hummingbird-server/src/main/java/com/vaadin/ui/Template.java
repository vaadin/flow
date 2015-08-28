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

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.JsonConverter;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.parser.TemplateParser;

import elemental.json.JsonArray;
import elemental.json.JsonType;
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

        getNode().put(TemplateCallbackHandler.class, this::onBrowserEvent);
    }

    protected void onBrowserEvent(StateNode node, String methodName,
            JsonArray params) {
        Method method = findTemplateEventHandlerMethod(getClass(), methodName,
                params.length());
        if (method == null) {
            throw new RuntimeException("Couldn't find any @"
                    + com.vaadin.annotations.TemplateEventHandler.class
                            .getName()
                    + " method named " + methodName + "with " + params.length()
                    + " parameters in " + getClass().getName());
        }

        int paramIndex = 0;

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] methodParams = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            JsonValue param = params.get(paramIndex++);
            if (type == Element.class && param.getType() == JsonType.ARRAY) {
                JsonArray elementArray = (JsonArray) param;
                int nodeId = (int) elementArray.getNumber(0);
                int templateId = (int) elementArray.getNumber(1);

                StateNode elementNode = node.getRoot().getById(nodeId);
                ElementTemplate template = getUI().getTemplate(templateId);
                methodParams[i] = Element.getElement(template, elementNode);
            } else {
                methodParams[i] = JsonConverter.fromJson(type, param);
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
            String methodName, int paramCount) {
        while (type != null && type != Template.class) {
            Method[] declaredMethods = type.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.getName().equals(methodName)
                        && method.getAnnotation(
                                TemplateEventHandler.class) != null
                        && method.getParameterCount() == paramCount) {
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
