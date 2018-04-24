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

import java.util.Objects;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

/**
 * An abstract implementation of a field, or a {@code Component} allowing user
 * input. Implements {@link HasValue} to represent the input value. Examples of
 * typical field components include text fields, date pickers, and check boxes.
 * <p>
 * The field value is represented in two separate ways. The field implementation
 * manages an internal representation of the value as shown to the user. This is
 * typically represented in the component's server-side DOM element or through
 * child components. The second representation is the value available for
 * programmatic use through the {@link HasValue} interface. This representation
 * is handled by this class and should not be directly accessed by subclasses.
 * <p>
 * In order to keep the two value representations in sync with each other,
 * subclasses must take care of three things. See the detailed documentation for
 * each method for further details.
 * <ol>
 * <li>Listen to changes from the user, and call
 * {@link #internalValueUpdated(Object, boolean)} with an updated value.
 * <li>Implement {@link #setInternalValue(Object)} to update the internal state
 * of the component to represent a new value.
 * </ol>
 *
 * @author Vaadin Ltd
 * @param <C>
 *            the source type for value change events
 * @param <T>
 *            the value type
 */
public abstract class AbstractField<C extends AbstractField<C, T>, T>
        extends Component implements HasValue<C, T>, HasEnabled {

    private final T initialValue;

    private T bufferedValue;

    private boolean inWriteValue;

    private boolean valueUpdatedInWriteValue;

    private T pendingInternalValue;

    /**
     * Creates a new field with an element created based on the {@link Tag}
     * annotation of the sub class. The provided initial value is by default
     * also used as {@link #getEmptyValue()}.
     *
     * @param initialValue
     *            the initial value
     */
    public AbstractField(T initialValue) {
        super();

        this.initialValue = initialValue;
        bufferedValue = initialValue;
    }

    /**
     * Creates a new field with the given element instance. The provided initial
     * value is by default also used as {@link #getEmptyValue()}.
     *
     * @param element
     *            the root element for the component
     * @param initialValue
     *            the initial value
     */
    public AbstractField(Element element, T initialValue) {
        super(element);

        this.initialValue = initialValue;
        bufferedValue = initialValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Registration addValueChangeListener(
            HasValue.ValueChangeListener<C, T> listener) {
        @SuppressWarnings("rawtypes")
        ComponentEventListener componentListener = event -> {
            ValueChangeEvent<C, T> valueChangeEvent = (ValueChangeEvent<C, T>) event;
            listener.onComponentEvent(valueChangeEvent);
        };
        return addListener(ValueChangeEvent.class, componentListener);
    }

    @Override
    public void setValue(T value) {
        setValue(value, false, false);
    }

    private void setValue(T newValue, boolean fromInternal,
            boolean fromClient) {
        if (fromClient && isReadOnly()) {
            applyValue(bufferedValue);
            return;
        }

        T oldValue = this.getValue();

        if (valueEquals(newValue, oldValue)) {
            return;
        }

        this.bufferedValue = newValue;

        if (!fromInternal) {
            boolean pendingInternalUpdated;
            try {
                pendingInternalUpdated = applyValue(newValue);
            } catch (RuntimeException e) {
                this.bufferedValue = oldValue;
                throw e;
            }

            if (pendingInternalUpdated) {
                if (valueEquals(pendingInternalValue, oldValue)) {
                    bufferedValue = oldValue;
                    return;
                }
                this.bufferedValue = pendingInternalValue;
            }
        }

        fireEvent(createValueChange(oldValue, fromClient));
    }

    private boolean applyValue(T value) {
        inWriteValue = true;
        valueUpdatedInWriteValue = false;

        try {
            setInternalValue(value);
        } finally {
            inWriteValue = false;
        }

        return valueUpdatedInWriteValue;
    }

    private ValueChangeEvent<C, T> createValueChange(T oldValue,
            boolean fromClient) {
        @SuppressWarnings("unchecked")
        C thisC = (C) this;

        return new ValueChangeEvent<>(thisC, this, oldValue, fromClient);
    }

    /**
     * Updates the internals of this field to represent the provided value.
     * Subclasses should override this method to show the value to the user.
     * This is typically done by setting an element property or by applying
     * changes to child components.
     * <p>
     * If {@link #internalValueUpdated(Object, boolean)} is called from within
     * this method, then the value provided in the latest invocation will be
     * used as the external value instead of the original value. In this case
     * {@link #setInternalValue(Object)} will not be called again. Changing the
     * provided value might be useful if the provided value is sanitized.
     *
     * @param value
     *            the new value to show
     */
    protected abstract void setInternalValue(T value);

    /**
     * Notifies that the internal value representation might have changed.
     * Subclasses should call this method whenever the internal value
     * representation changes. A value change event is fired if the new value is
     * different from the previous value according to
     * {@link #valueEquals(Object, Object)}.
     * <p>
     * If the value is marked as originated from the client and this field is in
     * readonly mode, then the new value will be ignored and
     * {@link #setInternalValue(Object)} will be called with the previous value.
     *
     * @param value
     *            the new internal value to use
     * @param fromClient
     *            <code>true</code> if the new value originates from the client;
     *            otherwise <code>false</code>
     */
    protected void internalValueUpdated(T value, boolean fromClient) {
        if (inWriteValue) {
            valueUpdatedInWriteValue = true;
            pendingInternalValue = value;
            return;
        }
        setValue(value, true, fromClient);
    }

    /**
     * Compares to value instances to each other to determine whether they are
     * equal. Equality is used to determine whether to update internal state and
     * fire an event when {@link #setValue(Object)} or
     * {@link #internalValueUpdated(Object, boolean)} is called. Subclasses can
     * override this method to define an alternative comparison method instead
     * of {@link Objects#equals(Object)}.
     *
     * @param value1
     *            the first instance
     * @param value2
     *            the second instance
     * @return <code>true</code> if the instances are equal; otherwise
     *         <code>false</code>
     */
    protected boolean valueEquals(T value1, T value2) {
        return Objects.equals(value1, value2);
    }

    @Override
    public T getValue() {
        return bufferedValue;
    }

    @Override
    public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        getElement().setProperty("required", requiredIndicatorVisible);
    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        return getElement().getProperty("required", false);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        getElement().setProperty("readonly", readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return getElement().getProperty("readonly", false);
    }

    @Override
    public T getEmptyValue() {
        return initialValue;
    }
}
