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
package com.vaadin.flow.component.webcomponent;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JsonCodec;

import elemental.json.Json;
import elemental.json.JsonString;

/**
 * Wrapper component for a WebComponent that exposes client callable methods
 * that the client side components expect to be available.
 */
public class WebComponentWrapper extends Component {

    private final Component child;
    private final Map<String, Field> fields;
    private final Map<String, Method> methods;

    /**
     * Wrapper class for the server side WebComponent.
     *
     * @param tag
     *         web component tag
     * @param child
     *         actual web component instance
     */
    public WebComponentWrapper(String tag, Component child) {
        super(new Element(tag));

        this.child = child;
        getElement().appendChild(child.getElement());

        this.fields = getPropertyFields(child.getClass());
        this.methods = getPropertyMethods(child.getClass());
    }

    /**
     * Synchronize method for client side to send property value updates to the
     * server.
     *
     * @param property
     *         property name to update
     * @param newValue
     *         the new value to set
     */
    @ClientCallable
    public void sync(String property, String newValue) {
        try {
            if (methods.containsKey(property)) {
                Method method = methods.get(property);
                boolean accessible = method.isAccessible();
                method.setAccessible(true);

                Class<?>[] parameterTypes = method.getParameterTypes();

                if (String.class.isAssignableFrom(parameterTypes[0])) {
                    method.invoke(child, newValue);
                } else {
                    JsonString value = Json.create(newValue);
                    if (JsonCodec.canEncodeWithoutTypeInfo(parameterTypes[0])) {
                        method.invoke(child,
                                JsonCodec.decodeAs(value, parameterTypes[0]));
                    } else {
                        throw new IllegalArgumentException(String.format(
                                "Received value wasn't convertible to '%s'",
                                parameterTypes[0].getName()));
                    }
                }

                method.setAccessible(accessible);
            } else if (fields.containsKey(property)) {
                Field field = fields.get(property);
                boolean accessible = field.isAccessible();
                field.setAccessible(true);
                WebComponentProperty fieldProperty = (WebComponentProperty) field
                        .get(child);

                if (String.class
                        .isAssignableFrom(fieldProperty.getValueType())) {
                    fieldProperty.set(newValue);
                } else {

                    JsonString value = Json.create(newValue);
                    if (JsonCodec.canEncodeWithoutTypeInfo(
                            fieldProperty.getValueType())) {
                        fieldProperty.set(JsonCodec
                                .decodeAs(value, fieldProperty.getValueType()));
                    } else {
                        throw new IllegalArgumentException(String.format(
                                "Received value wasn't convertible to '%s'",
                                fieldProperty.getValueType().getName()));
                    }
                }

                //                    ((WCProperty<Object>) field.get(child)).set(newValue);
                field.setAccessible(accessible);
            } else {
                System.err.println("No method found for " + property);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            LoggerFactory.getLogger(child.getClass())
                    .error("Failed to synchronise property '{}'", property, e);
        }
    }

    /**
     * Get all methods published to the client side as properties.
     *
     * @param webComponent
     *         component to get all methods for
     * @return map containing property name and {@link Method}
     */
    private static Map<String, Method> getPropertyMethods(
            Class<?> webComponent) {
        Map<String, Method> methods = new HashMap<>();

        for (Method method : webComponent.getDeclaredMethods()) {
            WebComponentMethod ann = method
                    .getAnnotation(WebComponentMethod.class);
            if (ann != null) {
                methods.put(ann.value(), method);
            }
        }
        return methods;
    }

    /**
     * Get all fields published to the client side as properties.
     *
     * @param webComponent
     *         component to get all fields for
     * @return map containing property name and {@link Field}
     */
    private static Map<String, Field> getPropertyFields(Class<?> webComponent) {
        Map<String, Field> fields = new HashMap<>();

        for (Field field : webComponent.getDeclaredFields()) {
            if (WebComponentProperty.class.isAssignableFrom(field.getType())) {
                fields.put(field.getName(), field);
            }
        }
        return fields;
    }
}
