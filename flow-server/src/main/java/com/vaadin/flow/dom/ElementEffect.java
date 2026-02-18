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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalEnvironment;
import com.vaadin.flow.signals.function.EffectAction;
import com.vaadin.flow.signals.impl.Effect;

/**
 * The utility class that provides helper methods for using Signal effects in a
 * context of a given element's life-cycle.
 * <p>
 * It ultimately creates a Signal effect, i.e. a call to
 * {@link Signal#unboundEffect(EffectAction)}, that is automatically enabled
 * when an element is attached and disabled when the element is detached.
 * Additionally, it provides methods to bind signals to element according to a
 * given value setting function.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public final class ElementEffect implements Serializable {
    private final SerializableRunnable effectFunction;
    private boolean closed = false;
    private Effect effect = null;
    private Registration detachRegistration;

    public ElementEffect(Element owner, SerializableRunnable effectFunction) {
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
     *             + someSignal.get());
     * });
     * effect.remove(); // to remove the effect when no longer needed
     * </pre>
     *
     * @see Signal#unboundEffect(EffectAction)
     * @param owner
     *            the owner element for which the effect is applied, must not be
     *            <code>null</code>
     * @param effectFunction
     *            the effect function to be executed when any dependency is
     *            changed, must not be <code>null</code>
     * @return a {@link Registration} that can be used to remove the effect
     *         function
     */
    public static Registration effect(Element owner,
            SerializableRunnable effectFunction) {
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
     * @see Signal#unboundEffect(EffectAction)
     * @param owner
     *            the owner element for which the effect is applied, must not be
     *            <code>null</code>
     * @param signal
     *            the signal whose value is to be bound to the element, must not
     *            be <code>null</code>
     * @param setter
     *            the setter function that defines how the signal value is
     *            applied to the element, must not be <code>null</code>
     * @return a {@link Registration} that can be used to remove the effect
     *         function
     * @param <T>
     *            the type of the signal value
     */
    public static <T> Registration bind(Element owner, Signal<T> signal,
            SerializableBiConsumer<Element, T> setter) {
        return effect(owner, () -> {
            setter.accept(owner, signal.get());
        });
    }

    private void enableEffect(Element owner) {
        if (closed) {
            return;
        }

        Component parentComponent = ComponentUtil.findParentComponent(owner)
                .get();
        UI ui = parentComponent.getUI().get();

        EffectAction errorHandlingEffectFunction = () -> {
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
     *
     * @param parentElement
     *            target parent element, must not be <code>null</code>
     * @param list
     *            list signal to bind to the parent, must not be
     *            <code>null</code>
     * @param childFactory
     *            factory to create new element, must not be <code>null</code>
     * @param <T>
     *            the value type of the {@link Signal}s in the list
     * @param <S>
     *            the type of the {@link Signal}s in the list
     * @throws IllegalStateException
     *             thrown if parent element isn't empty
     */
    public static <T, S extends Signal<T>> Registration bindChildren(
            Element parentElement, Signal<List<S>> list,
            SerializableFunction<S, Element> childFactory) {
        Objects.requireNonNull(parentElement, "Parent element cannot be null");
        Objects.requireNonNull(parentElement, "Parent element cannot be null");
        Objects.requireNonNull(list, "List signal cannot be null");
        Objects.requireNonNull(childFactory,
                "Child element factory cannot be null");

        if (parentElement.getChildCount() > 0) {
            throw new IllegalStateException(
                    "Parent element must not have children when binding a list signal to it");
        }
        // Create a child element cache outside the effect to persist elements
        // created by the child factory and avoid recreating them each time the
        // effect runs due to signal changes.
        HashMap<S, Element> valueSignalToChildCache = new HashMap<>();

        return new ElementEffect(parentElement,
                () -> runEffect(new BindChildrenEffectContext<T, S>(
                        parentElement, list.get(), childFactory,
                        valueSignalToChildCache)))::close;
    }

    private static <T, S extends Signal<T>> void runEffect(
            BindChildrenEffectContext<T, S> context) {
        // Cache the children to avoid multiple traversals
        LinkedList<Element> remainingChildren = context
                .parentChildrenToLinkedList();
        // Cache the children in a HashSet for O(1) lookups and removals
        HashSet<Element> remainingChildrenSet = new HashSet<>(
                remainingChildren);

        if (remainingChildren.size() != context.getCachedChildrenSize()) {
            throw new IllegalStateException(
                    "Parent element must have children matching the list signal. Unexpected child count: "
                            + remainingChildren.size() + ", expected: "
                            + context.getCachedChildrenSize());
        }
        removeNotPresentChildren(context, remainingChildrenSet);
        updateByChildSignals(context, remainingChildren, remainingChildrenSet);

        // Final validation to ensure no unexpected children are present. This
        // will catch also wrong order and is run as a last to avoid running
        // expensive validation in middle of the effect.
        validate(context);
    }

    /**
     * Validate that parent element has no children not belonging to the list of
     * child signals.
     */
    private static <T, S extends Signal<T>> void validate(
            BindChildrenEffectContext<T, S> context) {
        LinkedList<Element> children = context.parentChildrenToLinkedList();
        int index = 0;
        for (Element actualElement : children) {
            if (index >= context.childSignalsList.size()) {
                throw new IllegalStateException(String.format(
                        "Parent element must have children matching the list signal. Unexpected child at index %1$s: %2$s, expected: %3$s",
                        index, actualElement, "none"));
            }
            Element expectedElement = context.valueSignalToChildCache
                    .get(context.childSignalsList.get(index));
            if (!Objects.equals(actualElement, expectedElement)) {
                throw new IllegalStateException(String.format(
                        "Parent element must have children matching the list signal. Unexpected child at index %1$s: %2$s, expected: %3$s",
                        index, actualElement, expectedElement));
            }
            index++;
        }
        if (children.size() > context.getCachedChildrenSize()) {
            throw new IllegalStateException(String.format(
                    "Parent element must have children matching the list signal. Too many children: %1$s, expected: %2$s",
                    children.size(), context.getCachedChildrenSize()));
        }
    }

    /**
     * Remove all existing children in valueSignalToChildCache map that are no
     * longer present in the list of child signals.
     */
    private static <T, S extends Signal<T>> void removeNotPresentChildren(
            BindChildrenEffectContext<T, S> context,
            HashSet<Element> remainingChildrenSet) {
        var toRemove = new HashSet<>(context.valueSignalToChildCache.keySet());
        context.childSignalsList.forEach(toRemove::remove);
        for (S removedItem : toRemove) {
            Element element = context.valueSignalToChildCache
                    .remove(removedItem);
            element.removeFromParent();
            remainingChildrenSet.remove(element);
        }
    }

    /**
     * Align parent element children with the list of child signals without
     * removing any existing elements. Creates new elements with the element
     * factory if not found from the cache.
     */
    private static <T, S extends Signal<T>> void updateByChildSignals(
            BindChildrenEffectContext<T, S> context,
            LinkedList<Element> remainingChildren,
            HashSet<Element> remainingChildrenSet) {

        for (int i = 0; i < context.childSignalsList.size(); i++) {
            S item = context.childSignalsList.get(i);

            Element expectedChild = context.getElement(item);
            if (remainingChildrenSet.isEmpty() || !Objects
                    .equals(expectedChild.getParent(), context.parentElement)) {
                context.parentElement.insertChild(i, expectedChild);
                continue;
            }

            // Use LinkedList for order
            Element actualChild = remainingChildren.pollFirst();
            // Skip children that have been removed already
            while (actualChild != null
                    && !remainingChildrenSet.contains(actualChild)) {
                actualChild = remainingChildren.pollFirst();
            }
            if (actualChild == null) {
                continue;
            }
            if (!Objects.equals(actualChild, expectedChild)) {
                /*
                 * A mismatch has been encountered and we need to adjust the
                 * component children to match the expected order. This
                 * algorithm optimized for cases where only a single item has
                 * been moved to a new location and accepts that we might do
                 * redundant operations in other cases.
                 */
                if (Objects.equals(expectedChild, remainingChildren.peek())) {
                    // Move actual child to a later position
                    // Remove from current pos. Will be added back later
                    actualChild.removeFromParent();

                    // Skip next child since that's the expected child
                    remainingChildren.pollFirst();
                } else {
                    // Move expected child from a later position
                    context.parentElement.insertChild(i, expectedChild);

                    remainingChildrenSet.remove(expectedChild);

                    // Restore previous actual child for next round
                    remainingChildren.addFirst(actualChild);
                }
            }
        }
    }

    /**
     * Record for {@link #bindChildren(Element, Signal, SerializableFunction)}
     * effect to update children of a parent element according to a list of
     * child signals.
     *
     * @param parentElement
     *            parent element to update children for
     * @param childSignalsList
     *            list of child signals to update by
     * @param childElementFactory
     *            factory to create new child element
     * @param valueSignalToChildCache
     *            map to store existing child elements by value signal
     * @param <T>
     *            the value type of the list signal to update by
     * @param <S>
     *            the type of the signal in the list
     */
    private record BindChildrenEffectContext<T, S extends Signal<T>>(
            Element parentElement, List<S> childSignalsList,
            SerializableFunction<S, Element> childElementFactory,
            HashMap<S, Element> valueSignalToChildCache)
            implements
                Serializable {

        /**
         * Return existing element or generate new by child element factory.
         *
         * @throws IllegalStateException
         *             if child factory adds or removes unexpected child
         */
        private Element getElement(S item) {
            return valueSignalToChildCache.computeIfAbsent(item,
                    childElementFactory);
        }

        /**
         * Returns size of the <code>valueSignalToChildCache</code> map holding
         * cached child elements by value signal.
         */
        private int getCachedChildrenSize() {
            return valueSignalToChildCache.size();
        }

        private LinkedList<Element> parentChildrenToLinkedList() {
            return parentElement.getChildren()
                    .collect(Collectors.toCollection(LinkedList::new));
        }
    }
}
