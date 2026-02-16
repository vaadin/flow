/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.function.EffectAction;
import com.vaadin.flow.signals.impl.Effect;

/**
 * The utility class that provides helper methods for using Signal effects in a
 * context of a given component's life-cycle.
 * <p>
 * It ultimately creates a Signal effect, i.e. a call to
 * {@link Signal#effect(EffectAction)}, that is automatically enabled when a
 * component is attached and disabled when the component is detached.
 * Additionally it provides methods to bind signals to component according to a
 * given value setting function.
 *
 * @since 24.8
 */
public final class ComponentEffect implements Serializable {

    private ElementEffect elementEffect;

    private <C extends Component> ComponentEffect(C owner,
            SerializableRunnable effectFunction) {
        this.elementEffect = new ElementEffect(owner.getElement(),
                effectFunction);
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
     *             + someSignal.get());
     * });
     * effect.remove(); // to remove the effect when no longer needed
     * </pre>
     *
     * @see Signal#effect(EffectAction)
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
     * @deprecated Use {@link Effect#effect(Component, SerializableRunnable)}
     *             instead
     */
    @Deprecated(forRemoval = true)
    public static <C extends Component> Registration effect(C owner,
            SerializableRunnable effectFunction) {
        return Effect.effect(owner, effectFunction);
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
     * @see Signal#effect(EffectAction)
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
        return Effect.effect(owner, () -> setter.accept(owner, signal.get()));
    }

    /**
     * Binds a list signal containing child signals to a parent component using
     * a child component factory. Each signal in the list corresponds to a child
     * component within the parent.
     * <p>
     * The parent component is automatically updated to reflect the structure of
     * the list signal. Changes to the list, such as additions, removals, or
     * reordering, will update the parent's children accordingly.
     * <p>
     * The parent component must not contain any children that are not part of
     * the list signal. If the parent has existing children when this method is
     * called, or if it contains unrelated children after the list changes, an
     * {@link IllegalStateException} will be thrown.
     * <p>
     * New child components are created using the provided
     * <code>childFactory</code> function. This function takes a signal from the
     * list and returns a corresponding {@link Component}. It shouldn't return
     * <code>null</code>. The signal can be further bound to the returned
     * component as needed. Note that <code>childFactory</code> is run inside a
     * {@link Effect}, and therefore {@link Signal#get()} calls makes effect
     * re-run automatically on signal value change.
     * <p>
     * Example of usage:
     *
     * <pre>
     * SharedListSignal&lt;String&gt; taskList = new SharedListSignal&lt;&gt;(String.class);
     *
     * UnorderedList component = new UnorderedList();
     *
     * ComponentEffect.bindChildren(component, taskList, ListItem::new);
     * </pre>
     *
     * @param parent
     *            target parent component, must not be <code>null</code>
     * @param list
     *            list signal to bind to the parent, must not be
     *            <code>null</code>
     * @param childFactory
     *            factory to create new component, must not be <code>null</code>
     * @param <T>
     *            the value type of the {@link Signal}s in the list
     * @param <S>
     *            the type of the {@link Signal}s in the list
     * @param <P>
     *            the type of the parent component
     * @throws IllegalStateException
     *             thrown if parent component isn't empty
     */
    @Deprecated(forRemoval = true)
    public static <T, S extends Signal<T>, P extends Component & HasComponents> Registration bindChildren(
            P parent, Signal<List<S>> list,
            SerializableFunction<S, Component> childFactory) {
        Objects.requireNonNull(parent, "Parent component cannot be null");
        Objects.requireNonNull(list, "List signal cannot be null");
        Objects.requireNonNull(childFactory,
                "Child component factory cannot be null");
        return ElementEffect.bindChildren(parent.getElement(), list,
                // wrap childFactory to convert Component to Element
                signalValue -> Optional
                        .ofNullable(childFactory.apply(signalValue))
                        .map(Component::getElement)
                        .orElseThrow(() -> new IllegalStateException(
                                "ComponentEffect.bindChildren childFactory must not return null")));
    }

    private void close() {
        elementEffect.close();
        elementEffect = null;
    }

}
