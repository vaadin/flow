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
package com.vaadin.flow.data.binder;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Objects;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * Generic {@link HasValue} to use any type of component with Vaadin data
 * binding.
 * <p>
 * Example:
 * 
 * <pre>
 * Label label = new Label();
 * ReadOnlyHasValue&lt;String&gt; hasValue = new ReadOnlyHasValue&lt;&gt;(label::setText);
 * binder.forField(hasValue).bind(SomeBean::getName, null);
 * </pre>
 *
 * @param <V>
 *            the value type
 * @since 1.0
 */
public class ReadOnlyHasValue<V>
        implements HasValue<ValueChangeEvent<V>, V>, Serializable {
    private V value;
    private final SerializableConsumer<V> valueProcessor;
    private final V emptyValue;
    private LinkedHashSet<ValueChangeListener<? super ValueChangeEvent<V>>> listenerList;

    /**
     * Creates new {@code ReadOnlyHasValue}
     *
     * @param valueProcessor
     *            the value valueProcessor, e.g. a setter for displaying the
     *            value in a component
     * @param emptyValue
     *            the value to be used as empty, {@code null} by default
     */
    public ReadOnlyHasValue(SerializableConsumer<V> valueProcessor,
            V emptyValue) {
        this.valueProcessor = valueProcessor;
        this.emptyValue = emptyValue;
    }

    /**
     * Creates new {@code ReadOnlyHasValue} with {@code null} as an empty value.
     *
     * @param valueProcessor
     *            the value valueProcessor, e.g. a setter for displaying the
     *            value in a component
     */
    public ReadOnlyHasValue(SerializableConsumer<V> valueProcessor) {
        this(valueProcessor, null);
    }

    @Override
    public void setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        valueProcessor.accept(value);

        if (listenerList != null && !Objects.equals(oldValue, value)) {
            for (ValueChangeListener<? super ValueChangeEvent<V>> valueChangeListener : listenerList) {
                valueChangeListener.valueChanged(
                        new ReadOnlyValueChangeEvent<>(this, value, oldValue));
            }
        }
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public Registration addValueChangeListener(
            ValueChangeListener<? super ValueChangeEvent<V>> listener) {
        Objects.requireNonNull(listener, "Listener must not be null.");
        if (listenerList == null) {
            listenerList = new LinkedHashSet<>();
        }
        listenerList.add(listener);

        return () -> listenerList.remove(listener);
    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        return false;
    }

    @Override
    public void setRequiredIndicatorVisible(boolean requiredIndicatorVisible) {
        if (requiredIndicatorVisible) {
            throw new IllegalArgumentException("Not Writable");
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        if (!readOnly) {
            throw new IllegalArgumentException("Not Writable");
        }
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public V getEmptyValue() {
        return emptyValue;
    }

    private static class ReadOnlyValueChangeEvent<V>
            implements ValueChangeEvent<V> {

        private HasValue<?, V> hasValue;
        private V value;
        private V oldValue;

        public ReadOnlyValueChangeEvent(HasValue<?, V> hasValue, V value,
                V oldValue) {
            this.hasValue = hasValue;
            this.value = value;
            this.oldValue = oldValue;
        }

        @Override
        public HasValue<?, V> getHasValue() {
            return hasValue;
        }

        @Override
        public boolean isFromClient() {
            return false;
        }

        @Override
        public V getOldValue() {
            return oldValue;
        }

        @Override
        public V getValue() {
            return value;
        }
    }
}
