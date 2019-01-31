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
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.shared.Registration;

import elemental.json.JsonValue;

/**
 * Wrapper component for a WebComponent that exposes client callable methods
 * that the client side components expect to be available.
 */
public class WebComponentWrapper extends Component {

    private final Component child;
    private final HashMap<String, Field> fields;
    private final HashMap<String, Method> methods;

    // Disconnect timeout
    private Registration disconnectRegistration;
    private long disconnect;

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
    public void sync(String property, JsonValue newValue) {
        if (methods.containsKey(property) && fields.containsKey(property)) {
            String message = String
                    .format("The property '%s' exists both as a method and a field.",
                            property);
            throw new IllegalStateException(message);
        }
        try {
            if (methods.containsKey(property)) {
                setNewMethodValue(property, newValue);
            } else if (fields.containsKey(property)) {
                setNewFieldValue(property, newValue);
            } else {
                LoggerFactory.getLogger(child.getClass())
                        .error("No method found for {}", property);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            LoggerFactory.getLogger(child.getClass())
                    .error("Failed to synchronise property '{}'", property, e);
        }
    }

    /**
     * Cancel cleanup for a disconnected component.
     */
    @ClientCallable
    public void reconnect() {
        if (disconnectRegistration != null) {
            disconnectRegistration.remove();
        } else {
            LoggerFactory.getLogger(WebComponentUI.class)
                    .warn("Received reconnect request for non disconnected WebComponent '{}'",
                            this.child.getClass().getName());
        }
    }

    /**
     * A WebComponent disconnected from the dom will be scheduled for cleaning
     * if it doesn't get reconnected before times up.
     */
    @ClientCallable
    public void disconnected() {
        Optional<UI> uiOptional = getUI();

        if (uiOptional.isPresent() && disconnectRegistration == null) {
            disconnect = System.currentTimeMillis();
            disconnectRegistration = uiOptional.get().getInternals()
                    .addHeartbeatListener(event -> {
                        int disconnectTimeout = event.getSource().getSession()
                                .getConfiguration().getWebComponentDisconnect();

                        int timeout = 1000 * disconnectTimeout;

                        if (event.getSource().getInternals()
                                .getLastHeartbeatTimestamp() - disconnect
                                > timeout) {
                            this.getElement().removeFromParent();
                        }
                    });
        }
    }

    private void setNewMethodValue(String property, JsonValue newValue)
            throws IllegalAccessException, InvocationTargetException {
        Method method = methods.get(property);
        boolean accessible = method.isAccessible();
        method.setAccessible(true);

        Class<?>[] parameterTypes = method.getParameterTypes();

        if (JsonCodec.canEncodeWithoutTypeInfo(parameterTypes[0])) {
            method.invoke(child,
                    JsonCodec.decodeAs(newValue, parameterTypes[0]));
        } else {
            throw new IllegalArgumentException(
                    String.format("Received value wasn't convertible to '%s'",
                            parameterTypes[0].getName()));
        }

        method.setAccessible(accessible);
    }

    private void setNewFieldValue(String property, JsonValue newValue)
            throws IllegalAccessException {
        Field field = fields.get(property);
        boolean accessible = field.isAccessible();
        field.setAccessible(true);
        WebComponentProperty fieldProperty = (WebComponentProperty) field
                .get(child);

        if (JsonCodec.canEncodeWithoutTypeInfo(fieldProperty.getValueType())) {
            fieldProperty.set(JsonCodec
                    .decodeAs(newValue, fieldProperty.getValueType()));
        } else {
            throw new IllegalArgumentException(
                    String.format("Received value wasn't convertible to '%s'",
                            fieldProperty.getValueType().getName()));
        }

        field.setAccessible(accessible);
    }

    /**
     * Get all methods published to the client side as properties.
     *
     * @param webComponent
     *         component to get all methods for
     * @return map containing property name and {@link Method}
     */
    private static HashMap<String, Method> getPropertyMethods(
            Class<?> webComponent) {
        HashMap<String, Method> methods = new HashMap<>();

        // Collect first inherited methods so they can be overridden by the child
        if (webComponent.getSuperclass() != null) {
            methods.putAll(getPropertyMethods(webComponent.getSuperclass()));
        }

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
    private static HashMap<String, Field> getPropertyFields(
            Class<?> webComponent) {
        HashMap<String, Field> fields = new HashMap<>();

        // Collect first inherited methods so they can be overridden by the child
        if (webComponent.getSuperclass() != null) {
            fields.putAll(getPropertyFields(webComponent.getSuperclass()));
        }

        for (Field field : webComponent.getDeclaredFields()) {
            if (WebComponentProperty.class.isAssignableFrom(field.getType())) {
                fields.put(field.getName(), field);
            }
        }

        return fields;
    }
}
