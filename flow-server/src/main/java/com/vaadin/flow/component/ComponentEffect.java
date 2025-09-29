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

import java.util.Objects;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalEnvironment;
import com.vaadin.signals.impl.Effect;

/**
 * The utility class that provides helper methods for using Signal effects in a
 * context of a given component's life-cycle.
 * <p>
 * It ultimately creates a Signal effect, i.e. a call to
 * {@link Signal#effect(Runnable)}, that is automatically enabled when a
 * component is attached and disabled when the component is detached.
 * Additionally it provides methods to bind signals to component according to a
 * given value setting function.
 *
 * @since 24.8
 */
public final class ComponentEffect {
    private final Runnable effectFunction;
    private boolean closed = false;
    private Effect effect = null;

    private <C extends Component> ComponentEffect(C owner,
            Runnable effectFunction) {
        Objects.requireNonNull(owner, "Owner component cannot be null");
        Objects.requireNonNull(effectFunction,
                "Effect function cannot be null");
        this.effectFunction = effectFunction;
        owner.addAttachListener(attach -> {
            enableEffect(attach.getSource());

            owner.addDetachListener(detach -> {
                disableEffect();
                detach.unregisterListener();
            });
        });

        if (owner.isAttached()) {
            enableEffect(owner);
        }
    }

    /**
     * Creates a Signal effect that is owned by a given component. The effect is
     * enabled when the component is attached and automatically disabled when it
     * is detached.
     * <p>
     * Example of usage:
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

    private void enableEffect(Component owner) {
        if (closed) {
            return;
        }

        UI ui = owner.getUI().get();

        Runnable errorHandlingEffectFunction = () -> {
            try {
                effectFunction.run();
            } catch (Exception e) {
                ui.getSession().getErrorHandler()
                        .error(new ErrorEvent(e, owner.getElement().getNode()));
            }
        };

        assert effect == null;
        effect = new Effect(errorHandlingEffectFunction, command -> {
            if (UI.getCurrent() == ui) {
                // Run immediately if on the same UI
                command.run();
            } else {
                SignalEnvironment.getDefaultEffectDispatcher().execute(() -> {
                    try {
                        // Guard against detach while waiting for lock
                        if (effect != null) {
                            ui.access(command::run);
                        }
                    } catch (UIDetachedException e) {
                        // Effect was concurrently disabled -> nothing do to
                    }
                });
            }
        });
    }

    private void disableEffect() {
        if (effect != null) {
            effect.dispose();
            effect = null;
        }
    }

    private void close() {
        disableEffect();
        closed = true;
    }
}
