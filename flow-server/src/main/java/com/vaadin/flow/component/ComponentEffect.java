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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.SignalEnvironment;
import com.vaadin.signals.ValueSignal;
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

    /**
     * Binds {@link ListSignal} to parent component with a component factory.
     * Parent component mush implement {@link HasComponents} and it must be
     * subtype of {@link Component}.
     *
     * @param parent
     *            target parent component, must not be <code>null</code>
     * @param list
     *            list signal to bind to the parent, must not be
     *            <code>null</code>
     * @param childFactory
     *            factory to create new component, must not be <code>null</code>
     * @param <T>
     *            the value type
     * @param <PARENT>
     *            the type of the parent
     * @throws IllegalStateException
     *             thrown if parent component has children not belonging to the
     *             signal
     */
    public static <T, PARENT extends Component & HasComponents> void bindChildren(
            PARENT parent, ListSignal<T> list,
            SerializableFunction<ValueSignal<T>, Component> childFactory) {
        Objects.requireNonNull(parent, "Parent component cannot be null");
        Objects.requireNonNull(childFactory,
                "Child component factory cannot be null");
        bindChildren(parent, parent.getElement(), list,
                // wrap childFactory to convert Component to Element
                signalValue -> Optional
                        .ofNullable(childFactory.apply(signalValue))
                        .map(Component::getElement).orElse(null));
    }

    // TODO update JavaDoc
    /**
     * Binds {@link ListSignal} to parent element with an element factory.
     * Parent component is needed as a Signal effect owner.
     *
     * @param parentComponent
     *            target parent component as a Signal effect owner, must not be
     *            <code>null</code>
     * @param parent
     *            target parent element, must not be <code>null</code>
     * @param list
     *            list signal to bind to the parent, must not be
     *            <code>null</code>
     * @param childFactory
     *            factory to create new element, must not be <code>null</code>
     * @param <T>
     *            the value type
     * @throws IllegalStateException
     *             thrown if parent component has children not belonging to the
     *             signal
     */
    private static <T> void bindChildren(Component parentComponent,
            Element parent, ListSignal<T> list,
            SerializableFunction<ValueSignal<T>, Element> childFactory) {
        Objects.requireNonNull(parentComponent,
                "Parent component cannot be null");
        Objects.requireNonNull(parent, "Parent element cannot be null");
        Objects.requireNonNull(list, "ListSignal cannot be null");
        Objects.requireNonNull(childFactory,
                "Child element factory cannot be null");

        if (parent.getChildCount() > 0) {
            throw new IllegalStateException(
                    "Parent element must not have children when binding ListSignal to it");
        }
        HashMap<ValueSignal<T>, Element> valueSignalToChild = new HashMap<>();

        ComponentEffect.effect(parentComponent,
                () -> runEffect(new UpdateElementByChildSignals<>(parent,
                        list.value(), childFactory, valueSignalToChild)));
    }

    private static <T> void runEffect(UpdateElementByChildSignals<T> update) {
        update.doUpdate();
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

    /**
     * Helper record to update children of a parent element according to a list
     * of child signals.
     *
     * @param parentElement
     *            parent element to update children for
     * @param childSignals
     *            list of child signals to update by
     * @param childElementFactory
     *            factory to create new child element
     * @param valueSignalToChild
     *            map to store existing child elements by value signal
     * @param <T>
     *            the value type of the list signal to update by
     */
    private record UpdateElementByChildSignals<T>(Element parentElement,
            List<ValueSignal<T>> childSignals,
            SerializableFunction<ValueSignal<T>, Element> childElementFactory,
            HashMap<ValueSignal<T>, Element> valueSignalToChild)
            implements
                Serializable {

        /**
         * Return existing element or generate new by child element factory.
         */
        private Element getElement(ValueSignal<T> item) {
            return valueSignalToChild.computeIfAbsent(item,
                    childElementFactory);
        }

        /**
         * Update children of the parent element according to the list of child
         * signals.
         */
        private void doUpdate() {
            // Cache the children to avoid multiple traversals
            LinkedList<Element> remainingChildren = parentElement.getChildren()
                    .collect(Collectors.toCollection(LinkedList::new));

            validate(remainingChildren);
            removeNotPresentChildren();
            updateByChildSignals(remainingChildren);
        }

        /**
         * Validate that parent element has no children not belonging to the
         * list of child signals.
         */
        private void validate(LinkedList<Element> children) {
            if (children.stream().anyMatch(
                    element -> !valueSignalToChild.containsValue(element))) {
                throw new IllegalStateException(
                        "Parent element must not have children not belonging to the signal");
            }
        }

        /**
         * Remove all existing children in valueSignalToChild map that are no
         * longer present in the list of child signals.
         */
        private void removeNotPresentChildren() {
            var toRemove = new HashSet<>(valueSignalToChild.keySet());
            childSignals.forEach(toRemove::remove);
            for (ValueSignal<T> removedItem : toRemove) {
                Element element = valueSignalToChild.remove(removedItem);
                element.removeFromParent();
            }
        }

        /**
         * Align parent element children with the list of child signals without
         * removing any existing elements. Creates new elements with the element
         * factory if not found from the cache.
         */
        private void updateByChildSignals(
                LinkedList<Element> remainingChildren) {
            // Cache the children in a HashSet for O(1) lookups and removals
            HashSet<Element> remainingChildrenSet = new HashSet<>(
                    remainingChildren);

            for (int i = 0; i < childSignals.size(); i++) {
                ValueSignal<T> item = childSignals.get(i);

                Element expectedChild = getElement(item);
                if (remainingChildrenSet.isEmpty() || !Objects
                        .equals(expectedChild.getParent(), parentElement)) {
                    parentElement.insertChild(i, expectedChild);
                    continue;
                }

                // Use LinkedList for order
                Element actualChild = remainingChildren.pollFirst();
                if (!Objects.equals(actualChild, expectedChild)) {
                    /*
                     * A mismatch has been encountered and we need to adjust the
                     * component children to match the expected order. This
                     * algorithm optimized for cases where only a single item
                     * has been moved to a new location and accepts that we
                     * might do redundant operations in other cases.
                     */
                    if (expectedChild == remainingChildren.peek()) {
                        // Move actual child to a later position

                        // Remove from current pos. Will be added back later
                        actualChild.removeFromParent();

                        // Skip next child since that's the expected child
                        remainingChildren.pollFirst();
                    } else {
                        // Move expected child from a later position
                        parentElement.insertChild(i, expectedChild);

                        remainingChildrenSet.remove(expectedChild);

                        // Restore previous actual child for next round
                        remainingChildren.addFirst(actualChild);
                    }
                }
            }
        }
    }
}
