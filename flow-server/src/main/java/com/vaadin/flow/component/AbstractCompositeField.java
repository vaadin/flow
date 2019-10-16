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

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.internal.AbstractFieldSupport;
import com.vaadin.flow.shared.Registration;

/**
 * An abstract field class that is backed by a composite component.
 * <p>
 * Note that composite fields do not automatically show client side
 * validation error messages or required indicators.
 * <p>
 * See the detailed documentation for {@link AbstractField} and
 * {@link Composite} for detailed information.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <C>
 *            the type of the content component
 * @param <S>
 *            the source type for value change events
 * @param <T>
 *            the value type
 */
public abstract class AbstractCompositeField<C extends Component, S extends AbstractCompositeField<C, S, T>, T>
        extends Composite<C> implements
        HasValueAndElement<ComponentValueChangeEvent<S, T>, T>, HasEnabled {

    private final AbstractFieldSupport<S, T> fieldSupport;

    /**
     * Creates a new field. The provided default value is used by
     * {@link #getEmptyValue()} and is also used as the initial value of this
     * instance.
     *
     * @param defaultValue
     *            the default value
     */
    public AbstractCompositeField(T defaultValue) {
        @SuppressWarnings("unchecked")
        S thisAsS = (S) this;

        fieldSupport = new AbstractFieldSupport<>(thisAsS, defaultValue,
                this::valueEquals, this::setPresentationValue);
    }

    @Override
    public void setValue(T value) {
        fieldSupport.setValue(value);
    }

    @Override
    public T getValue() {
        return fieldSupport.getValue();
    }

    @Override
    public T getEmptyValue() {
        return fieldSupport.getEmptyValue();
    }

    @Override
    public boolean isEmpty() {
        return valueEquals(getValue(), getEmptyValue());
    }

    @Override
    public Registration addValueChangeListener(
            HasValue.ValueChangeListener<? super ComponentValueChangeEvent<S, T>> listener) {
        return fieldSupport.addValueChangeListener(listener);
    }

    /**
     * Updates the presentation of this field to display the provided value.
     * Subclasses should override this method to show the value to the user.
     * This is typically done by setting an element property or by applying
     * changes to child components.
     * <p>
     * If {@link #setModelValue(Object, boolean)} is called from within this
     * method, then value of the last invocation will be used as the model value
     * instead of the value passed to this method. In this case
     * {@link #setPresentationValue(Object)} will not be called again. Changing
     * the provided value might be useful if the provided value is sanitized.
     * <p>
     * See {@link AbstractField} for an overall description on the difference
     * between model values and presentation values.
     *
     * @param newPresentationValue
     *            the new value to show
     */
    protected abstract void setPresentationValue(T newPresentationValue);

    /**
     * Updates the model value if the value has actually changed. Subclasses
     * should call this method whenever the user has changed the value. A value
     * change event is fired if the new value is different from the previous
     * value according to {@link #valueEquals(Object, Object)}.
     * <p>
     * If the value is from the client-side and this field is in readonly mode,
     * then the new model value will be ignored.
     * {@link #setPresentationValue(Object)} will be called with the previous
     * model value so that the representation shown to the user can be reverted.
     * <p>
     * See {@link AbstractField} for an overall description on the difference
     * between model values and presentation values.
     *
     * @param newModelValue
     *            the new internal value to use
     * @param fromClient
     *            <code>true</code> if the new value originates from the client;
     *            otherwise <code>false</code>
     */
    protected void setModelValue(T newModelValue, boolean fromClient) {
        fieldSupport.setModelValue(newModelValue, fromClient);
    }

    /**
     * Compares to value instances to each other to determine whether they are
     * equal. Equality is used to determine whether to update internal state and
     * fire an event when {@link #setValue(Object)} or
     * {@link #setModelValue(Object, boolean)} is called. Subclasses can
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
        return fieldSupport.valueEquals(value1, value2);
    }

}
