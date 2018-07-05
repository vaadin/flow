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
package com.vaadin.flow.component.internal;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.function.SerializableBiPredicate;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * Encapsulates all the logic required for a typical field implementation. This
 * reduces the risk of implementing logic changes in {@link AbstractField} but
 * not in {@link AbstractCompositeField}, or vice versa.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <C>
 *            the value change source type
 * @param <T>
 *            the value type
 */
public class AbstractFieldSupport<C extends Component & HasValue<ComponentValueChangeEvent<C, T>, T>, T>
        implements Serializable {
    private final T defaultValue;
    private final C component;

    private final SerializableBiPredicate<T, T> valueEquals;
    private final SerializableConsumer<T> setPresentationValue;

    private T bufferedValue;

    private boolean presentationUpdateInProgress;
    private boolean valueSetFromPresentationUpdate;
    private T pendingValueFromPresentation;

    /**
     * Creates a new field support.
     *
     * @param component
     *            the owning field component
     * @param defaultValue
     *            the default field value
     * @param valueEquals
     *            a callback for comparing values
     * @param setPresentationValue
     *            a callback for setting presentation values
     */
    public AbstractFieldSupport(C component, T defaultValue,
            SerializableBiPredicate<T, T> valueEquals,
            SerializableConsumer<T> setPresentationValue) {
        this.component = component;

        this.defaultValue = defaultValue;
        bufferedValue = defaultValue;
        this.valueEquals = valueEquals;
        this.setPresentationValue = setPresentationValue;
    }

    /**
     * Delegate method for
     * {@link HasValue#addValueChangeListener(com.vaadin.flow.component.HasValue.ValueChangeListener)}
     *
     * @param listener
     *            the listener to add
     * @return a registration for the listener
     */
    @SuppressWarnings("unchecked")
    public Registration addValueChangeListener(
            HasValue.ValueChangeListener<? super ComponentValueChangeEvent<C, T>> listener) {

        @SuppressWarnings("rawtypes")
        ComponentEventListener componentListener = event -> {
            ComponentValueChangeEvent<C, T> valueChangeEvent = (ComponentValueChangeEvent<C, T>) event;
            listener.valueChanged(valueChangeEvent);
        };
        return ComponentUtil.addListener(component,
                ComponentValueChangeEvent.class, componentListener);
    }

    private ComponentValueChangeEvent<C, T> createValueChange(T oldValue,
            boolean fromClient) {
        return new ComponentValueChangeEvent<>(component, component, oldValue,
                fromClient);
    }

    /**
     * Delegate method for {@link HasValue#getValue()}.
     *
     * @return the field value
     */
    public T getValue() {
        return bufferedValue;
    }

    /**
     * Delegate method for {@link HasValue#getEmptyValue()}.
     *
     * @return the empty value of this field
     */
    public T getEmptyValue() {
        return defaultValue;
    }

    /**
     * Delegate method for {@link HasValue#setValue(Object)}.
     *
     * @param value
     *            the value to set
     */
    public void setValue(T value) {
        setValue(value, false, false);
    }

    /**
     * Delegate method corresponding to
     * {@link AbstractField#valueEquals(Object, Object)}.
     *
     * @param value1
     *            the first instance
     * @param value2
     *            the second instance
     * @return <code>true</code> if the instances are equal; otherwise
     *         <code>false</code>
     */
    public boolean valueEquals(T value1, T value2) {
        return Objects.equals(value1, value2);
    }

    /**
     * Delegate method corresponding to
     * {@link AbstractField#setModelValue(Object, boolean)}.
     *
     * @param newModelValue
     *            the new internal value to use
     * @param fromClient
     *            <code>true</code> if the new value originates from the client;
     *            otherwise <code>false</code>
     */
    public void setModelValue(T newModelValue, boolean fromClient) {
        if (presentationUpdateInProgress) {
            valueSetFromPresentationUpdate = true;
            pendingValueFromPresentation = newModelValue;
            return;
        }
        setValue(newModelValue, true, fromClient);
    }

    private void setValue(T newValue, boolean fromInternal,
            boolean fromClient) {
        if (fromClient && component.isReadOnly()) {
            applyValue(bufferedValue);
            return;
        }

        T oldValue = getValue();

        if (valueEquals.test(newValue, oldValue)) {
            return;
        }

        bufferedValue = newValue;

        if (!fromInternal) {
            boolean pendingInternalUpdated;
            try {
                pendingInternalUpdated = applyValue(newValue);
            } catch (RuntimeException e) {
                bufferedValue = oldValue;
                throw e;
            }

            /*
             * Regardless of what sonar believes, this will be true in cases
             * when setPresentationValue calls setModelValue.
             */
            if (pendingInternalUpdated) {
                if (valueEquals.test(pendingValueFromPresentation, oldValue)) {
                    bufferedValue = oldValue;
                    return;
                }
                bufferedValue = pendingValueFromPresentation;
            }
        }

        ComponentUtil.fireEvent(component,
                createValueChange(oldValue, fromClient));
    }

    private boolean applyValue(T value) {
        presentationUpdateInProgress = true;
        /*
         * Toggled to true by setModelValue if that method is run while
         * presentationUpdateInProgress is also true.
         */
        valueSetFromPresentationUpdate = false;

        try {
            setPresentationValue.accept(value);
        } finally {
            presentationUpdateInProgress = false;
        }

        return valueSetFromPresentationUpdate;
    }
}
