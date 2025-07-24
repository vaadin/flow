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
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.internal.LocaleUtil;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.NumberSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.ValueSignal;

/**
 * The utility class that provides helper methods for using Signal effects in a
 * context of a given component's life-cycle.
 * <p>
 * It ultimately creates a Signal effect, i.e. a call to
 * {@link Signal#effect(Runnable)}, that is automatically enabled when a
 * component is attached and disabled when the component is detached.
 * Additionally it provides methods to bind signals to component according to a
 * given value settng function and format strings based on signal values.
 *
 * @since 24.8
 */
public final class ComponentEffect {
    private final Runnable effectFunction;
    private boolean closed = false;
    private Runnable effectShutdown = null;

    private <C extends Component> ComponentEffect(C owner,
            Runnable effectFunction) {
        Objects.requireNonNull(owner, "Owner component cannot be null");
        Objects.requireNonNull(effectFunction,
                "Effect function cannot be null");
        this.effectFunction = effectFunction;
        owner.addAttachListener(attach -> {
            enableEffect();

            owner.addDetachListener(detach -> {
                disableEffect();
                detach.unregisterListener();
            });
        });

        if (owner.isAttached()) {
            enableEffect();
        }
    }

    /**
     * Creates a Signal effect that is owned by a given component. The effect is
     * enabled when the component is attached and automatically disabled when it
     * is detached.
     * <p>
     * Examle of usage:
     *
     * <pre>
     * Registration effect = ComponentEffect.effect(myComponent, () -> {
     *     Notification.show("Component is attached and signal value is "
     *             + someSignal.value());
     * });
     * effect.remove(); // to remove the effect when no longer needed
     * </pre>
     *
     * @see Signal#effect(Runnable)
     * @param <C>
     *            the type of the component
     * @param owner
     *            the owner component for which the effect is applied, must not
     *            be <code>null</code>
     * @param effectFunction
     *            the effect function to be executed when any dependency is
     *            changed, must not be <code>null</code>
     * @return a {@link Registration} that can be used to remove the effect
     *         function
     */
    public static <C extends Component> Registration effect(C owner,
            Runnable effectFunction) {
        ComponentEffect effect = new ComponentEffect(owner, effectFunction);
        return effect::close;
    }

    /**
     * Binds a <code>signal</code>'s value to a given owner component in a way
     * defined in <code>setter</code> function and creates a Signal effect
     * function executing the setter whenever the signal value changes.
     * <p>
     * Example of usage:
     *
     * <pre>
     * Registration effect = ComponentEffect.bind(mySpan, stringSignal,
     *         Span::setText);
     * effect.remove(); // to remove the effect when no longer needed
     *
     * ComponentEffect.bind(mySpan, stringSignal.map(value -> !value.isEmpty()),
     *         Span::setVisible);
     * </pre>
     *
     * @see Signal#effect(Runnable)
     * @param owner
     *            the owner component for which the effect is applied, must not
     *            be <code>null</code>
     * @param signal
     *            the signal whose value is to be bound to the component, must
     *            not be <code>null</code>
     * @param setter
     *            the setter function that defines how the signal value is
     *            applied to the component, must not be <code>null</code>
     * @return a {@link Registration} that can be used to remove the effect
     *         function
     * @param <C>
     *            the type of the component
     * @param <T>
     *            the type of the signal value
     */
    public static <C extends Component, T> Registration bind(C owner,
            Signal<T> signal, SerializableBiConsumer<C, T> setter) {
        return effect(owner, () -> {
            setter.accept(owner, signal.value());
        });
    }

    /**
     * Formats a string using the values of the provided signals and the given
     * locale, sets the formatted string on the owner component using the
     * provided setter function.
     * <p>
     * Binds a formatted string using the values of the provided signals to a
     * given owner component in a way defined in <code>setter</code> function
     * and creates a Signal effect function executing the setter whenever the
     * signal value changes.
     * <p>
     * Example of usage:
     *
     * <pre>
     * ComponentEffect.format(mySpan, Span::setText, Locale.US,
     *         "The price of %s is %.2f", nameSignal, priceSignal);
     * </pre>
     *
     * @see Signal#effect(Runnable)
     * @param owner
     *            the owner component for which the effect is applied, must not
     *            be <code>null</code>
     * @param setter
     *            the setter function that defines how the formatted string is
     *            applied to the component, must not be <code>null</code>
     * @param locale
     *            the locale to be used for formatting the string, if
     *            <code>null</code>, then no localization is applied
     * @param format
     *            the format string to be used for formatting the signal values,
     *            must not be <code>null</code>
     * @param signals
     *            the signals whose values are to be used for formatting the
     *            string, must not be <code>null</code>
     * @return a {@link Registration} that can be used to remove the effect
     *         function
     * @param <C>
     *            the type of the component
     */
    public static <C extends Component> Registration format(C owner,
            SerializableBiConsumer<C, String> setter, Locale locale,
            String format, Signal<?>... signals) {
        return effect(owner, () -> {
            Object[] values = Stream.of(signals).map(Signal::value).toArray();
            setter.accept(owner, String.format(locale, format, values));
        });
    }

    /**
     * Formats a string using the values of the provided signals and sets it on
     * the owner component using the provided setter function.
     * <p>
     * Binds a formatted string using the values of the provided signals to a
     * given owner component in a way defined in <code>setter</code> function
     * and creates a Signal effect function executing the setter whenever the
     * signal value changes.
     * <p>
     * Formats using locale from the current UI, I18NProvider or default locale
     * depending on what is available.
     * <p>
     * Example of usage:
     *
     * <pre>
     * ComponentEffect.format(mySpan, Span::setText, "The price of %s is %.2f",
     *         nameSignal, priceSignal);
     * </pre>
     *
     * @see Signal#effect(Runnable)
     * @param owner
     *            the owner component for which the effect is applied, must not
     *            be <code>null</code>
     * @param setter
     *            the setter function that defines how the formatted string is
     *            applied to the component, must not be <code>null</code>
     * @param format
     *            the format string to be used for formatting the signal values,
     *            must not be <code>null</code>
     * @param signals
     *            the signals whose values are to be used for formatting the
     *            string, must not be <code>null</code>
     * @return a {@link Registration} that can be used to remove the effect
     *         function
     * @param <C>
     *            the type of the component
     */
    public static <C extends Component> Registration format(C owner,
            SerializableBiConsumer<C, String> setter, String format,
            Signal<?>... signals) {
        Locale locale = LocaleUtil.getLocale();
        return format(owner, setter, locale, format, signals);
    }

    private void enableEffect() {
        if (closed) {
            return;
        }

        assert effectShutdown == null;
        effectShutdown = Signal.effect(effectFunction);
    }

    private void disableEffect() {
        if (effectShutdown != null) {
            effectShutdown.run();
            effectShutdown = null;
        }
    }

    private void close() {
        disableEffect();
        closed = true;
    }
}
