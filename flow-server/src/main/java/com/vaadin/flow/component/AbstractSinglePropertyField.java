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
package com.vaadin.flow.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.PropertyChangeEvent;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.shared.util.SharedUtil;

import elemental.json.JsonValue;

/**
 * Abstract field that is based on a single element property.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <C>
 *            the source type for value change events
 * @param <T>
 *            the value type
 */
public class AbstractSinglePropertyField<C extends AbstractField<C, T>, T>
        extends AbstractField<C, T> {
    @SuppressWarnings("rawtypes")
    private static final SerializableBiFunction RAW_IDENTITY = (ignore,
            value) -> value;

    @SuppressWarnings("rawtypes")
    private static final SerializableBiFunction RAW_NON_NULL_IDENTITY = (ignore,
            value) -> Objects.requireNonNull(value,
                    "Null value is not supported");

    @FunctionalInterface
    // Helper since Java has no TriConsumer
    private interface ElementSetter<T> extends Serializable {
        void setElementValue(Element element, String propertyName, T value);
    }

    @FunctionalInterface
    // Helper since Java has no TriFunction
    private interface ElementGetter<T> extends Serializable {
        T getValue(Element element, String propertyName, T defaultValue);
    }

    /**
     * Encapsulates everything related to reading and writing element properties
     * of a given type and converting them to a model type.
     *
     * @param <P>
     *            the element property type
     */
    private static class TypeHandler<P> implements Serializable {
        private final ElementSetter<P> setter;
        private final SerializableBiFunction<Element, String, P> getter;

        private TypeHandler(ElementSetter<P> setter, ElementGetter<P> getter,
                P typeDefault) {
            this.setter = setter;
            this.getter = (element, propertyName) -> getter.getValue(element,
                    propertyName, typeDefault);
        }

        private <C extends AbstractField<C, V>, V> SerializableBiFunction<C, V, V> createReader(
                Element element, String propertyName,
                SerializableBiFunction<C, P, V> presentationToModel) {
            return (component, defaultModelValue) -> {
                if (element.getPropertyRaw(propertyName) != null) {
                    P presentationValue = getter.apply(element, propertyName);

                    return presentationToModel.apply(component,
                            presentationValue);
                } else {
                    return defaultModelValue;
                }
            };
        }

        private <C extends AbstractField<C, V>, V> SerializableBiConsumer<C, V> createWriter(
                Element element, String propertyName,
                SerializableBiFunction<C, V, P> modelToPresentation) {
            return (component, modelValue) -> {
                P presentationValue = modelToPresentation.apply(component,
                        modelValue);

                if (presentationValue == null) {
                    element.removeProperty(propertyName);
                } else {
                    setter.setElementValue(element, propertyName,
                            presentationValue);
                }
            };
        }
    }

    private static final Map<Class<?>, TypeHandler<?>> typeHandlers = new HashMap<>();

    static {
        addHandler(Element::setProperty, Element::getProperty, String.class,
                "");
        addHandler(Element::setProperty, Element::getProperty, Double.class,
                Double.valueOf(0));
        addHandler(Element::setProperty, Element::getProperty, Boolean.class,
                Boolean.FALSE);
        addHandler(Element::setProperty, Element::getProperty, Integer.class,
                Integer.valueOf(0));
        typeHandlers.put(JsonValue.class, getHandler(JsonValue.class));
    }

    private final SerializableBiConsumer<C, T> propertyWriter;
    private final SerializableBiFunction<C, T, T> propertyReader;
    private final String propertyName;

    private DomListenerRegistration synchronizationRegistration;

    /**
     * Creates a new field that uses a property value without any conversion.
     * <p>
     * The value type of the class must be one of the types that can be written
     * as an element property value: String, Integer, Double or Boolean.
     *
     * @param propertyName
     *            the name of the element property to use
     * @param defaultValue
     *            the default value to use if the property isn't defined
     * @param acceptNullValues
     *            if <code>true</code>, an exception will be thrown if the model
     *            value is set to <code>null</code>; if <code>false</code> the
     *            property will be removed when the model value is set to
     *            <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public AbstractSinglePropertyField(String propertyName, T defaultValue,
            boolean acceptNullValues) {
        this(propertyName, defaultValue, null, RAW_IDENTITY,
                acceptNullValues ? RAW_IDENTITY : RAW_NON_NULL_IDENTITY);
    }

    /**
     * Creates a new field that uses a property value with the given stateless
     * converters for producing a model value.
     * <p>
     * The property type must be one of the types that can be written as an
     * element property value: String, Integer, Double or Boolean.
     *
     * @param propertyName
     *            the name of the element property to use
     * @param defaultValue
     *            the default value to use if the property isn't defined
     * @param elementPropertyType
     *            the type of the element property
     * @param presentationToModel
     *            a function that converts a property value to a model value
     * @param modelToPresentation
     *            a function that converts a model value to a property value
     * @param <P>
     *            the property type
     */
    public <P> AbstractSinglePropertyField(String propertyName, T defaultValue,
            Class<P> elementPropertyType,
            SerializableFunction<P, T> presentationToModel,
            SerializableFunction<T, P> modelToPresentation) {
        this(propertyName, defaultValue, elementPropertyType,
                (ignore, value) -> presentationToModel.apply(value),
                (ignore, value) -> modelToPresentation.apply(value));
    }

    /**
     * Creates a new field that uses a property value with the given contextual
     * converters for producing a model value.
     * <p>
     * The property type must be one of the types that can be written as an
     * element property value: String, Integer, Double or Boolean.
     *
     * @param propertyName
     *            the name of the element property to use
     * @param defaultValue
     *            the default value to use if the property isn't defined
     * @param elementPropertyType
     *            the type of the element property
     * @param presentationToModel
     *            a function that accepts this component and a property value
     *            and returns a model value
     * @param modelToPresentation
     *            a function that accepts this component and a model value and
     *            returns a property value
     * @param <P>
     *            the property type
     */
    public <P> AbstractSinglePropertyField(String propertyName, T defaultValue,
            Class<P> elementPropertyType,
            SerializableBiFunction<C, P, T> presentationToModel,
            SerializableBiFunction<C, T, P> modelToPresentation) {
        super(defaultValue);
        this.propertyName = propertyName;

        if (elementPropertyType == null) {
            if (presentationToModel == RAW_IDENTITY) {
                elementPropertyType = findElementPropertyTypeFromTypeParameter(
                        getClass());
                if (elementPropertyType == null) {
                    throw new IllegalStateException(
                            "Cannot automatically determine element property type based on type parameters.");
                }
            } else {
                throw new IllegalArgumentException(
                        "Element property type cannot be null");
            }
        }

        TypeHandler<P> typeHandler = findHandler(elementPropertyType);

        Element element = getElement();

        propertyWriter = typeHandler.createWriter(element, propertyName,
                modelToPresentation);
        propertyReader = typeHandler.createReader(element, propertyName,
                presentationToModel);

        doSetSynchronizedEvent(
                SharedUtil.camelCaseToDashSeparated(propertyName) + "-changed");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <P> TypeHandler<P> findHandler(Class<P> clazz) {
        TypeHandler<P> typeHandler = (TypeHandler<P>) typeHandlers.get(clazz);
        if (typeHandler == null && JsonValue.class.isAssignableFrom(clazz)) {
            typeHandler = getHandler((Class) clazz);
        }
        if (typeHandler == null) {
            throw new IllegalArgumentException(
                    "Unsupported element property type: " + clazz.getName()
                            + ". Supported types are: "
                            + typeHandlers.keySet().parallelStream()
                                    .map(Class::getName)
                                    .collect(Collectors.joining(", ")));
        }
        return typeHandler;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> findElementPropertyTypeFromTypeParameter(
            Class<?> hasValueClass) {
        return (Class<T>) GenericTypeReflector
                .erase(GenericTypeReflector.getTypeParameter(hasValueClass,
                        HasValue.class.getTypeParameters()[1]));
    }

    /**
     * Sets the name of the DOM event for which property values are synchronized
     * from the client to the server. By default, the event name is the property
     * name with <code>-changed</code> appended. This means that if the property
     * name is <code>value</code>, then the event default name is
     * <code>value-changed</code>.
     *
     * @see Element#addPropertyChangeListener(String, String,
     *      PropertyChangeListener)
     * @see #getSynchronizationRegistration()
     *
     * @param synchronizedEvent
     *            the property name to synchronize, or <code>null</code> to
     *            disable property synchronization
     */
    protected void setSynchronizedEvent(String synchronizedEvent) {
        doSetSynchronizedEvent(synchronizedEvent);
    }

    /**
     * Returns the registration of the DOM event listener that synchronizes the
     * property value. The registration is created by
     * {@link #setSynchronizedEvent(String)}.
     *
     * @return the registration of the DOM event listener that synchronizes the
     *         property value, or <code>null</code> if property synchronization
     *         is disabled
     */
    protected DomListenerRegistration getSynchronizationRegistration() {
        return synchronizationRegistration;
    }

    // Delegated method so that constructor doesn't have to call protected
    // method
    private void doSetSynchronizedEvent(String propChangeEvent) {
        if (synchronizationRegistration != null) {
            synchronizationRegistration.remove();
        }
        if (propChangeEvent != null) {
            synchronizationRegistration = getElement()
                    .addPropertyChangeListener(propertyName, propChangeEvent,
                            // This explicit class instantiation is the
                            // workaround
                            // which fixes a JVM optimization+serialization bug.
                            // Do not convert to lambda.
                            // See #5973.
                            new PropertyChangeListener() {

                                @Override
                                public void propertyChange(
                                        PropertyChangeEvent event) {
                                    handlePropertyChange(event);
                                }
                            });
        } else {
            synchronizationRegistration = null;
        }
    }

    /**
     * Checks whether the element property has a value that can be converted to
     * the model type. Property changes from the element will be ignored if this
     * method returns <code>false</code>. The default implementation always
     * return <code>true</code>.
     *
     * @return <code>true</code> if the element property value can be converted
     *         to the model type; otherwise <code>false</code>
     */
    protected boolean hasValidValue() {
        return true;
    }

    private void handlePropertyChange(PropertyChangeEvent event) {
        if (hasValidValue()) {
            @SuppressWarnings("unchecked")
            T presentationValue = propertyReader.apply((C) this,
                    getEmptyValue());

            setModelValue(presentationValue, event.isUserOriginated());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setPresentationValue(T newPresentationValue) {
        propertyWriter.accept((C) this, newPresentationValue);
    }

    private static <P extends JsonValue> TypeHandler<P> getHandler(
            Class<P> type) {
        ElementGetter<P> getter = (element, property, defaultValue) -> {
            Serializable value = element.getPropertyRaw(property);
            // JsonValue is passed straight through, other primitive
            // values are jsonified
            return type.cast(JsonCodec.encodeWithoutTypeInfo(value));
        };
        ElementSetter<P> setter = (element, property, value) -> element
                .setPropertyJson(property, value);
        return new TypeHandler<P>(setter, getter, null);
    }

    private static <T> void addHandler(ElementSetter<T> setter,
            ElementGetter<T> getter, Class<T> type, T typeDefaultValue) {
        typeHandlers.put(type,
                new TypeHandler<>(setter, getter, typeDefaultValue));
    }

}
