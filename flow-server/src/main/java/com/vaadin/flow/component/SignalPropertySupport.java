/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.util.Objects;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

/**
 * Helper class for binding a {@link Signal} to a property of a
 * {@link Component}. Not all component features delegate directly to the state
 * in {@link com.vaadin.flow.dom.Element}. For those features, this helper class
 * ensures state management behaves consistently with Element properties.
 * <p>
 * Example of usage:
 *
 * <pre>
 * ValueSignal&lt;String&gt; signal = new ValueSignal&lt;&gt;("");
 * MyComponent component = new MyComponent();
 * add(component);
 * component.bindTextContent(signal);
 * signal.set("Hello"); // component content showing now "Content: Hello" text
 * </pre>
 * 
 * <pre>
 * &#064;Tag(div)
 * public class MyComponent extends Component {
 *     private final SignalPropertySupport&lt;String&gt; textProperty = SignalPropertySupport
 *             .create(this, value -> {
 *                 getElement().executeJs("this.textContent = 'Content: ' + $0",
 *                         value);
 *             });
 *
 *     public String getText() {
 *         return textProperty.get();
 *     }
 *
 *     public void setText(String text) {
 *         textProperty.set(text);
 *     }
 *
 *     public void bindTextContent(Signal&lt;String&gt; textSignal) {
 *         textProperty.bind(textSignal);
 *     }
 * }
 * </pre>
 *
 * @param <T>
 *            the type of the property
 */
public class SignalPropertySupport<T> implements Serializable {

    private final SerializableConsumer<T> valueChangeConsumer;

    private final Component owner;

    private Registration registration;

    private Signal<T> signal;

    private T value;

    private SignalPropertySupport(Component owner,
            SerializableConsumer<T> valueChangeConsumer) {
        this.owner = Objects.requireNonNull(owner,
                "Owner component cannot be null");
        this.valueChangeConsumer = Objects.requireNonNull(valueChangeConsumer,
                "Value change consumer cannot be null");
    }

    /**
     * Creates a new instance of SignalPropertySupport for the given owner
     * component and a value change consumer to be called when property value is
     * updated. The property value is updated either manually with
     * {@link #set(Object)}, or automatically via {@link Signal} value change
     * while the owner component is in the attached state and signal is bound
     * with {@link #bind(Signal)}.
     *
     * @param owner
     *            the owner component for which the value change consumer is
     *            applied, must not be null
     * @param valueChangeConsumer
     *            the consumer to be called when the value changes, must not be
     *            null
     * @param <T>
     *            the type of the property
     * @return a new instance of SignalPropertySupport
     * @see #bind(Signal)
     */
    public static <T> SignalPropertySupport<T> create(Component owner,
            SerializableConsumer<T> valueChangeConsumer) {
        return new SignalPropertySupport<>(owner, valueChangeConsumer);
    }

    /**
     * Binds a {@link Signal}'s value to this property support and keeps the
     * value synchronized with the signal value while the component is in
     * attached state. When the component is in detached state, signal value
     * changes have no effect. <code>null</code> signal unbinds existing
     * binding.
     * <p>
     * While a Signal is bound to a property support, any attempt to set value
     * manually throws {@link com.vaadin.signals.BindingActiveException}. Same
     * happens when trying to bind a new Signal while one is already bound.
     *
     * @param signal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @throws com.vaadin.signals.BindingActiveException
     *             thrown when there is already an existing binding
     */
    public void bind(Signal<T> signal) {
        if (signal != null && this.signal != null) {
            throw new BindingActiveException();
        }
        this.signal = signal;
        if (signal == null && registration != null) {
            registration.remove();
            registration = null;
        }
        if (signal != null) {
            registration = ComponentEffect.effect(owner, () -> {
                value = signal.value();
                valueChangeConsumer.accept(value);
            });
        }
    }

    /**
     * Gets the current value of this property support.
     *
     * @return the current value
     */
    public T get() {
        return value;
    }

    /**
     * Sets the value of this property support.
     *
     * @param value
     *            the value to set
     * @throws com.vaadin.signals.BindingActiveException
     *             thrown when there is an existing binding
     */
    public void set(T value) {
        if (signal != null) {
            throw new BindingActiveException();
        }
        this.value = value;
        valueChangeConsumer.accept(value);
    }
}
