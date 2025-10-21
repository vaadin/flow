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
package com.vaadin.flow.dom;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalEnvironment;
import com.vaadin.signals.impl.Effect;

/**
 * The utility class that provides helper methods for using Signal effects in a
 * context of a given element's life-cycle.
 * <p>
 * It ultimately creates a Signal effect, i.e. a call to
 * {@link Signal#effect(Runnable)}, that is automatically enabled when a element
 * is attached and disabled when the element is detached. Additionally, it
 * provides methods to bind signals to element according to a given value
 * setting function.
 *
 * @since 25.0
 */
public final class ElementEffect {
    private final Runnable effectFunction;
    private boolean closed = false;
    private Effect effect = null;
    private Registration detachRegistration;

    public ElementEffect(Element owner, Runnable effectFunction) {
        Objects.requireNonNull(owner, "Owner element cannot be null");
        Objects.requireNonNull(effectFunction,
                "Effect function cannot be null");
        this.effectFunction = effectFunction;
        owner.addAttachListener(attach -> {
            enableEffect(attach.getSource());

            detachRegistration = owner.addDetachListener(detach -> {
                disableEffect();
                detachRegistration.remove();
                detachRegistration = null;
            });
        });

        if (owner.getNode().isAttached()) {
            enableEffect(owner);

            detachRegistration = owner.addDetachListener(detach -> {
                disableEffect();
                detachRegistration.remove();
                detachRegistration = null;
            });
        }
    }

    /**
     * Creates a Signal effect that is owned by a given element. The effect is
     * enabled when the element is attached and automatically disabled when it
     * is detached.
     * <p>
     * Example of usage:
     *
     * <pre>
     * Registration effect = ElementEffect.effect(myElement, () -> {
     *     Notification.show("Element is attached and signal value is "
     *             + someSignal.value());
     * });
     * effect.remove(); // to remove the effect when no longer needed
     * </pre>
     *
     * @see Signal#effect(Runnable)
     * @param owner
     *            the owner element for which the effect is applied, must not be
     *            <code>null</code>
     * @param effectFunction
     *            the effect function to be executed when any dependency is
     *            changed, must not be <code>null</code>
     * @return a {@link Registration} that can be used to remove the effect
     *         function
     */
    public static Registration effect(Element owner, Runnable effectFunction) {
        ElementEffect effect = new ElementEffect(owner, effectFunction);
        return effect::close;
    }

    /**
     * Binds a <code>signal</code>'s value to a given owner element in a way
     * defined in <code>setter</code> function and creates a Signal effect
     * function executing the setter whenever the signal value changes.
     * <p>
     * Example of usage:
     *
     * <pre>
     * Element mySpan = new Element("span");
     * Registration effect = ElementEffect.bind(mySpan, stringSignal,
     *         Element::setText);
     * effect.remove(); // to remove the effect when no longer needed
     *
     * ElementEffect.bind(mySpan, stringSignal.map(value -> !value.isEmpty()),
     *         Element::setVisible);
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
     * @param <T>
     *            the type of the signal value
     */
    public static <T> Registration bind(Element owner, Signal<T> signal,
            SerializableBiConsumer<Element, T> setter) {
        return effect(owner, () -> {
            setter.accept(owner, signal.value());
        });
    }

    private void enableEffect(Element owner) {
        if (closed) {
            return;
        }

        Component parentComponent = ComponentUtil.findParentComponent(owner)
                .get();
        UI ui = parentComponent.getUI().get();

        Runnable errorHandlingEffectFunction = () -> {
            try {
                effectFunction.run();
            } catch (Exception e) {
                ui.getSession().getErrorHandler()
                        .error(new ErrorEvent(e, owner.getNode()));
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

    public void close() {
        disableEffect();
        closed = true;
    }
}
