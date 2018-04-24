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
 * <li>Listen to potential value changes from the user, and call
 * {@link #valueUpdatedFromClient()} when the value might have changed.
 * <li>Implement {@link #readValue()} to return the current internal value of
 * the component.
 * <li>Implement {@link #writeValue(Object)} to update the internal state of the
 * component to represent a new value.
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

    private T bufferedValue;

    private boolean valueInitialized;

    private boolean inWriteValue;

    private boolean clientUpdateFromWriteValue;

    /**
     * Creates a new field with an element created based on the {@link Tag}
     * annotation of the sub class.
     */
    public AbstractField() {
        super();
    }

    /**
     * Creates a new field with the given element instance.
     *
     * @param element
     *            the root element for the component
     */
    public AbstractField(Element element) {
        super(element);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Registration addValueChangeListener(
            HasValue.ValueChangeListener<C, T> listener) {
        initValueIfNeeded();

        @SuppressWarnings("rawtypes")
        ComponentEventListener componentListener = event -> {
            ValueChangeEvent<C, T> valueChangeEvent = (ValueChangeEvent<C, T>) event;
            listener.onComponentEvent(valueChangeEvent);
        };
        return addListener(ValueChangeEvent.class, componentListener);
    }

    private void initValueIfNeeded() {
        if (!valueInitialized) {
            if (mayReadValue()) {
                this.bufferedValue = readValue();
            } else {
                this.bufferedValue = getEmptyValue();
            }
            valueInitialized = true;
        }
    }

    @Override
    public void setValue(T value) {
        setValue(value, false);
    }

    private boolean setValue(T newValue, boolean fromClient) {
        if (fromClient && isReadOnly()) {
            applyValue(bufferedValue);
            return false;
        }

        T oldValue = this.getValue();

        if (valueEquals(newValue, oldValue)) {
            return false;
        }

        this.bufferedValue = newValue;

        if (!fromClient) {
            boolean reentrantUpdate;
            try {
                reentrantUpdate = applyValue(newValue);
            } catch (RuntimeException e) {
                this.bufferedValue = oldValue;
                throw e;
            }
            if (reentrantUpdate && mayReadValue()) {
                T updatedValue = readValue();
                if (valueEquals(oldValue, updatedValue)) {
                    bufferedValue = oldValue;
                    return false;
                }
                bufferedValue = updatedValue;
            }
        }

        fireEvent(createValueChange(oldValue, fromClient));

        return true;
    }

    private boolean applyValue(T value) {
        inWriteValue = true;
        clientUpdateFromWriteValue = false;
        try {
            writeValue(value);
        } finally {
            inWriteValue = false;
        }

        return clientUpdateFromWriteValue;
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
     * The implementation may call {@link #valueUpdatedFromClient()} if the
     * update is not done in a way that makes {@link #readValue()} return the
     * provided value. In this case, {@link #readValue()} is run again and that
     * value will be used as the new field value. In this case
     * {@link #writeValue(Object)} will not be called again. Changing the
     * provided value might be useful if the provided value is sanitized.
     *
     * @param value
     *            the new value to show
     */
    protected abstract void writeValue(T value);

    /**
     * Retrieves the current value based on the internal representation of the
     * component. Subclasses should override this method to return a
     * representation of the value that is shown to the user. This is typically
     * done by reading an element property or combining values from child
     * components.
     * <p>
     * If the component internals can be in a state for which a valid value
     * cannot be produced, the subclass should also override
     * {@link #mayReadValue()} to control whether {@link #readValue()} can
     * currently be called.
     *
     * @return the internal value representation
     */
    protected abstract T readValue();

    /**
     * Notifies that the internal value representation might have changed.
     * Subclasses should call this method whenever the internal value
     * representation returned from {@link #readValue()} might have changed. A
     * value change event is fired if the new value is different from the
     * previous value according to {@link #valueEquals(Object, Object)}.
     *
     * @return <code>true</code> if the value was changed and an event was
     *         fired; otherwise <code>false</code>
     */
    protected boolean valueUpdatedFromClient() {
        if (inWriteValue) {
            clientUpdateFromWriteValue = true;
            return false;
        }
        return mayReadValue() && setValue(readValue(), true);
    }

    /**
     * Checks whether the internal value representation is currently in a
     * consistent state that allows running {@link #readValue()} without errors.
     * This method is by default implemented to always return <code>true</code>.
     * Subclasses can override it to perform internal validation. Returning
     * <code>false</code> from this method signals that {@link #readValue()}
     * should not be called and that {@link #valueUpdatedFromClient()} should
     * not proceed processing a potentially updated value.
     *
     * @return <code>true</code> if the internal value representation is
     *         consistent; otherwise <code>false</code>
     */
    protected boolean mayReadValue() {
        return true;
    }

    /**
     * Compares to value instances to each other to determine whether they are
     * equal. Equality is used to determine whether to update internal state and
     * fire an event when {@link #setValue(Object)} or
     * {@link #valueUpdatedFromClient()} is called. Subclasses can override this
     * method to define an alternative comparison method instead of
     * {@link Objects#equals(Object)}.
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
        initValueIfNeeded();

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
        // Ensure there's a previous value that can be restored
        initValueIfNeeded();
        getElement().setProperty("readonly", readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return getElement().getProperty("readonly", false);
    }

}
