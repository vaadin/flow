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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

/**
 * An abstract implementation of a field, or a {@code Component} allowing user
 * input. Implements {@link HasValue} to represent the input value. Examples of
 * typical field components include text fields, date pickers, and check boxes.
 * <p>
 * The field value is represented in two separate ways:
 * <ol>
 * <li>A presentation value that is shown to the user. This is typically
 * represented in the component's server-side DOM element or through child
 * components.
 * <li>A model value that is available for programmatic use through the
 * {@link HasValue} interface. This representation is handled by this class and
 * should not be directly accessed by subclasses.
 * </ol>
 * <p>
 * In order to keep the two value representations in sync with each other,
 * subclasses must take care of the two following things:
 * <ol>
 * <li>Listen to changes from the user, and call
 * {@link #setModelValue(Object, boolean)} with an updated value.
 * <li>Implement {@link #setPresentationValue(Object)} to update the
 * presentation value of the component so that the new value is shown to the
 * user.
 * </ol>
 * See the detailed documentation for the two methods for further details.
 * <p>
 * This class extends {@link Component}, which means that it cannot be used for
 * adding field functionality to an existing component without changing the
 * superclass of that component. As an alternative, you can use
 * {@link AbstractCompositeField} to instead wrap an instance of the existing
 * component.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <C>
 *            the source type for value change events
 * @param <T>
 *            the value type
 */
public abstract class AbstractField<C extends AbstractField<C, T>, T>
        extends Component
        implements HasValueAndElement<ComponentValueChangeEvent<C, T>, T> {

    /**
     * Value change event fired by components.
     *
     * @author Vaadin Ltd
 * @since 1.0
     * @param <C>
     *            the source component type
     * @param <V>
     *            the value type
     */
    public static class ComponentValueChangeEvent<C extends Component, V>
            extends ComponentEvent<C> implements HasValue.ValueChangeEvent<V> {

        private final V oldValue;
        private final V value;
        private final HasValue<?, V> hasValue;

        /**
         * Creates a new component value change event.
         *
         * @param source
         *            the source component
         * @param hasValue
         *            the HasValue from which the value originates
         * @param oldValue
         *            the old value
         * @param fromClient
         *            whether the value change originated from the client
         */
        public ComponentValueChangeEvent(C source, HasValue<?, V> hasValue,
                V oldValue, boolean fromClient) {
            super(source, fromClient);
            this.hasValue = hasValue;
            this.oldValue = oldValue;
            this.value = hasValue.getValue();
        }

        @Override
        public V getOldValue() {
            return oldValue;
        }

        @Override
        public V getValue() {
            return value;
        }

        /**
         * {@inheritDoc}
         * <p>
         * This is typically the same instance as {@link #getSource()}, but in
         * some cases the {@link HasValue} implementation is separated from the
         * component implementation.
         */
        @Override
        public HasValue<?, V> getHasValue() {
            return hasValue;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[source=" + source
                    + ", value = " + value + ", oldValue = " + oldValue + "]";
        }
    }

    private final AbstractFieldSupport<C, T> fieldSupport;

    /**
     * Creates a new field with an element created based on the {@link Tag}
     * annotation of the sub class. The provided default value is used by
     * {@link #getEmptyValue()} and as the initial model value of this instance.
     *
     * @param defaultValue
     *            the default value for fields of this type
     */
    public AbstractField(T defaultValue) {
        super();

        fieldSupport = createFieldSupport(defaultValue);
    }

    /**
     * Creates a new field with the given element instance. he provided default
     * value is used by {@link #getEmptyValue()} and as the initial model value
     * of this instance.
     *
     * @param element
     *            the root element for the component
     * @param defaultValue
     *            the default value for fields of this type
     */
    public AbstractField(Element element, T defaultValue) {
        super(element);

        fieldSupport = createFieldSupport(defaultValue);
    }

    private AbstractFieldSupport<C, T> createFieldSupport(T defaultValue) {
        @SuppressWarnings("unchecked")
        C thisC = (C) this;
        return new AbstractFieldSupport<>(thisC, defaultValue,
                this::valueEquals, this::setPresentationValue);
    }

    @Override
    public Registration addValueChangeListener(
            HasValue.ValueChangeListener<? super ComponentValueChangeEvent<C, T>> listener) {
        return fieldSupport.addValueChangeListener(listener);
    }

    @Override
    public void setValue(T value) {
        fieldSupport.setValue(value);
    }

    /**
     * Updates the presentation of this field to display the provided value.
     * Subclasses should override this method to show the value to the user.
     * This is typically done by setting an element property or by applying
     * changes to child components.
     * <p>
     * If {@link #setModelValue(Object, boolean)} is called from within this
     * method, the value of the last invocation will be used as the model value
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

    @Override
    public boolean isEmpty() {
        return valueEquals(getValue(), getEmptyValue());
    }

    @Override
    public T getValue() {
        return fieldSupport.getValue();
    }

    @Override
    public T getEmptyValue() {
        return fieldSupport.getEmptyValue();
    }
}
