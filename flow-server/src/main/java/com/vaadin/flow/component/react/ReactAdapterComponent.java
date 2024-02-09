/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.flow.component.react;

import com.fasterxml.jackson.core.type.TypeReference;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.JsonUtils;
import elemental.json.Json;
import elemental.json.JsonValue;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An abstract implementation of an adapter for integrating with React
 * components. To be used together with a React adapter Web Component that
 * subclasses the {@code ReactAdapterElement} JS class. The React adapter Web
 * Component defines the React JSX template to render the React components with
 * the specified props mapping, defines the named state that is synchronised
 * with the server-side Java component, and custom DOM events.
 * <p>
 * The subclasses should specify the following:
 * <ul>
 * <li>A {@link com.vaadin.flow.component.Tag} annotation with the name of the
 * React adapter Web Component.
 * <li>A {@link com.vaadin.flow.component.dependency.JsModule} annotation with
 * the React adapter Web Component implementation.
 * <li>An optional {@link com.vaadin.flow.component.dependency.NpmPackage}
 * annotation for npm dependencies of the React adapter Web Component.
 * </ul>
 *
 * @author Vaadin Ltd
 * @since 24.4
 */
public abstract class ReactAdapterComponent extends Component {
    /**
     * Adds the specified listener for the state change event in the React
     * adapter.
     *
     * @param stateName
     *            state name
     * @param typeClass
     *            type class of the state value
     * @param listener
     *            the listener callback for receiving value changes
     * @return listener registration object
     * @param <T>
     *            type of the state value
     */
    protected <T> DomListenerRegistration addStateChangeListener(
            String stateName, Class<T> typeClass, Consumer<T> listener) {
        return addJsonReaderStateChangeListener(stateName,
                (jsonValue -> readFromJson(jsonValue, typeClass)), listener);
    }

    /**
     * Adds the specified listener for the state change event in the React
     * adapter.
     *
     * @param stateName
     *            state name
     * @param typeReference
     *            type reference of the state value
     * @param listener
     *            the listener callback for receiving value changes
     * @return listener registration object
     * @param <T>
     *            type of the state value
     */
    protected <T> DomListenerRegistration addStateChangeListener(
            String stateName, TypeReference<T> typeReference,
            Consumer<T> listener) {
        return addJsonReaderStateChangeListener(stateName,
                (jsonValue -> readFromJson(jsonValue, typeReference)),
                listener);
    }

    /**
     * Assigns new value for the state in the React adapter.
     *
     * @param stateName
     *            state name
     * @param value
     *            value to assign
     * @param <T>
     *            type of the state value
     */
    protected <T> void setState(String stateName, T value) {
        getElement().setPropertyJson(stateName, writeAsJson(value));
    }

    /**
     * Reads the state value from the React adapter.
     *
     * @param stateName
     *            state name
     * @param typeClass
     *            type class of the state value
     * @return the current value
     * @param <T>
     *            type of the state value
     */
    protected <T> T getState(String stateName, Class<T> typeClass) {
        return readFromJson(getPropertyJson(stateName), typeClass);
    }

    /**
     * Reads the state value from the React adapter.
     *
     * @param stateName
     *            state name
     * @param typeReference
     *            type reference of the state value
     * @return the current value
     * @param <T>
     *            type of the state value
     */
    protected <T> T getState(String stateName, TypeReference<T> typeReference) {
        return readFromJson(getPropertyJson(stateName), typeReference);
    }

    /**
     * Converts JsonValue into Java object of given type.
     *
     * @param jsonValue
     *            JSON value to convert, not {@code null}
     * @param typeClass
     *            type class of converted object instance
     * @return converted object instance
     * @param <T>
     *            type of result instance
     */
    protected static <T> T readFromJson(JsonValue jsonValue,
            Class<T> typeClass) {
        return JsonUtils.readValue(jsonValue, typeClass);
    }

    /**
     * Converts JsonValue into Java object of given type.
     *
     * @param jsonValue
     *            JSON value to convert, not {@code null}
     * @param typeReference
     *            type reference of converted object instance
     * @return converted object instance
     * @param <T>
     *            type of result instance
     */
    protected static <T> T readFromJson(JsonValue jsonValue,
            TypeReference<T> typeReference) {
        return JsonUtils.readValue(jsonValue, typeReference);
    }

    /**
     * Converts Java object into JsonValue.
     *
     * @param object
     *            Java object to convert
     * @return converted JSON value
     */
    protected static JsonValue writeAsJson(Object object) {
        return JsonUtils.writeValue(object);
    }

    private JsonValue getPropertyJson(String propertyName) {
        var rawValue = getElement().getPropertyRaw(propertyName);
        if (rawValue == null) {
            return Json.createNull();
        } else if (rawValue instanceof JsonValue jsonValue) {
            return jsonValue;
        } else if (rawValue instanceof String stringValue) {
            return Json.create(stringValue);
        } else if (rawValue instanceof Double doubleValue) {
            return Json.create(doubleValue);
        } else if (rawValue instanceof Boolean booleanValue) {
            return Json.create(booleanValue);
        } else {
            return Json.create(rawValue.toString());
        }
    }

    private <T> DomListenerRegistration addJsonReaderStateChangeListener(
            String stateName, Function<JsonValue, T> jsonReader,
            Consumer<T> listener) {
        return getElement().addPropertyChangeListener(stateName,
                stateName + "-changed", (event -> {
                    JsonValue newStateJson = JsonCodec
                            .encodeWithoutTypeInfo(event.getValue());
                    T newState = jsonReader.apply(newStateJson);
                    listener.accept(newState);
                }));
    }

}
