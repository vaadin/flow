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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.PropertyChangeEvent;
import com.vaadin.flow.function.SerializableBiFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Abstract field that is based on a single element property.
 *
 * @author Vaadin Ltd
 * @param <C>
 *            the source type for value change events
 * @param <T>
 *            the value type
 */
public class AbstractSinglePropertyField<C extends AbstractField<C, T>, T>
        extends AbstractField<C, T> {
    @SuppressWarnings("rawtypes")
    private static final SerializableFunction RAW_IDENTITY = value -> value;

    @SuppressWarnings("rawtypes")
    private static final SerializableFunction RAW_NON_NULL_IDENTITY = value -> Objects
            .requireNonNull(value, "Null value is not supported");

    @FunctionalInterface
    // Helper since Java has no TriConsumer
    private interface ElementSetter<T> extends Serializable {
        public void setElementValue(Element element, String propertyName,
                T value);
    }

    @FunctionalInterface
    // Helper since Java has no TriFunction
    private interface ElementGetter<T> extends Serializable {
        public T getValue(Element element, String propertyName, T defaultValue);
    }

    /**
     * Encapsulates all everything related to reading and writing element
     * properties of a given type and converting them to a model type.
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

        private <V> SerializableFunction<V, V> createReader(Element element,
                String propertyName,
                SerializableFunction<P, V> presentationToModel) {

            return defaultModelValue -> {
                if (element.getPropertyRaw(propertyName) != null) {
                    P presentationValue = getter.apply(element, propertyName);

                    return presentationToModel.apply(presentationValue);
                } else {
                    return defaultModelValue;
                }
            };
        }

        private <V> SerializableConsumer<V> createWriter(Element element,
                String propertyName,
                SerializableFunction<V, P> modelToPresentation) {
            return modelValue -> {
                P presentationValue = modelToPresentation.apply(modelValue);

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

    private static <T> void addHandler(ElementSetter<T> setter,
            ElementGetter<T> getter, Class<T> type, T typeDefaultValue) {
        typeHandlers.put(type,
                new TypeHandler<>(setter, getter, typeDefaultValue));
    }

    static {
        addHandler(Element::setProperty, Element::getProperty, String.class,
                "");
        addHandler(Element::setProperty, Element::getProperty, Double.class,
                Double.valueOf(0));
        addHandler(Element::setProperty, Element::getProperty, Boolean.class,
                Boolean.FALSE);
        addHandler(Element::setProperty, Element::getProperty, Integer.class,
                Integer.valueOf(0));
    }

    private final SerializableConsumer<T> propertyWriter;
    private final SerializableFunction<T, T> propertyReader;
    private final String propertyName;

    private String synchronizedEvent;

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
     *            whether <code>null</code> is accepted as a model value
     */
    @SuppressWarnings("unchecked")
    public AbstractSinglePropertyField(String propertyName, T defaultValue,
            boolean acceptNullValues) {
        this(propertyName, defaultValue, null, RAW_IDENTITY,
                acceptNullValues ? RAW_IDENTITY : RAW_NON_NULL_IDENTITY);
    }

    /**
     * Creates a new field that uses a property value with the given converters
     * for producing a model value.
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

        @SuppressWarnings("unchecked")
        TypeHandler<P> typeHandler = (TypeHandler<P>) typeHandlers
                .get(elementPropertyType);
        if (typeHandler == null) {
            throw new IllegalArgumentException(
                    "Unsupported element property type: "
                            + elementPropertyType.getName()
                            + ". Supported types are: "
                            + typeHandlers.keySet().parallelStream()
                                    .map(Class::getName)
                                    .collect(Collectors.joining(", ")));
        }

        Element element = getElement();

        propertyWriter = typeHandler.createWriter(element, propertyName,
                modelToPresentation);
        propertyReader = typeHandler.createReader(element, propertyName,
                presentationToModel);

        element.addPropertyChangeListener(propertyName,
                this::handlePropertyChange);

        doSetSynchronizedEvent(propertyName + "-changed");
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
     * from the client to the server. By default, the event event name is the
     * property name with <code>-changed</code> appended. This means that if the
     * property name is <code>value</code>, then the event default name is
     * <code>value-changed</code>.
     *
     * @see Element#addSynchronizedPropertyEvent(String)
     *
     * @param synchronizedEvent
     *            the property name to synchronize, or <code>null</code> to
     *            disable property synchronization
     */
    protected void setSynchronizedEvent(String synchronizedEvent) {
        doSetSynchronizedEvent(synchronizedEvent);
    }

    // Delegated method so that constructor doens't have to call protected
    // method
    private void doSetSynchronizedEvent(String newEvent) {
        Element element = getElement();
        if (this.synchronizedEvent != null) {
            element.removeSynchronizedPropertyEvent(this.synchronizedEvent);
            element.removeSynchronizedProperty(propertyName);
        }

        this.synchronizedEvent = newEvent;

        if (newEvent != null) {
            element.synchronizeProperty(propertyName, newEvent);
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
            T presentationValue = propertyReader.apply(getEmptyValue());

            setModelValue(presentationValue, event.isUserOriginated());
        }
    }

    @Override
    protected void setPresentationValue(T newPresentationValue) {
        propertyWriter.accept(newPresentationValue);
    }
}
