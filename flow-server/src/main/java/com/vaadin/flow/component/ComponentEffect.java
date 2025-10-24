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
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.Signal;
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

    private ElementEffect elementEffect;

    private <C extends Component> ComponentEffect(C owner,
            Runnable effectFunction) {
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
     * Binds a {@link ListSignal} to a parent component using a child component
     * factory. Each {@link ValueSignal} in the list corresponds to a child
     * component within the parent.
     * <p>
     * The parent component is automatically updated to reflect the structure of
     * the {@link ListSignal}. Changes to the list, such as additions, removals,
     * or reordering, will update the parent's children accordingly.
     * <p>
     * The parent component must not contain any children that are not part of
     * the {@link ListSignal}. If the parent has existing children when this
     * method is called, or if it contains unrelated children after the list
     * changes, an {@link IllegalStateException} will be thrown.
     * <p>
     * New child components are created using the provided
     * <code>childFactory</code> function. This function takes a
     * {@link ValueSignal} from the {@link ListSignal} and returns a
     * corresponding {@link Component}. It shouldn't return <code>null</code>.
     * The {@link ValueSignal} can be further bound to the returned component as
     * needed. Note that <code>childFactory</code> is run inside a
     * {@link Effect}, and therefore {@link ValueSignal#value()} calls makes
     * effect re-run automatically on signal value change.
     * <p>
     * Example of usage:
     *
     * <pre>
     * ListSignal&lt;String&gt; taskList = new ListSignal&lt;&gt;(String.class);
     *
     * UnorderedList component = new UnorderedList();
     *
     * ComponentEffect.bindChildren(component, taskList, taskValueSignal -> {
     *     var listItem = new ListItem();
     *     ComponentEffect.bind(listItem, taskValueSignal, HasString::setText);
     *     return listItem;
     * });
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
     *            the value type of the {@link ValueSignal}s in the
     *            {@link ListSignal}
     * @param <PARENT>
     *            the type of the parent component
     * @throws IllegalStateException
     *             thrown if parent component isn't empty
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
                        .map(Component::getElement)
                        .orElseThrow(() -> new IllegalStateException(
                                "ComponentEffect.bindChildren childFactory must not return null")));
    }

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
        // Create a child element cache outside the effect to persist elements
        // created by the child factory and avoid recreating them each time the
        // effect runs due to signal changes.
        HashMap<ValueSignal<T>, Element> valueSignalToChildCache = new HashMap<>();

        ComponentEffect.effect(parentComponent,
                () -> runEffect(new BindChildrenEffectContext<>(parent,
                        list.value(), childFactory, valueSignalToChildCache)));
    }

    private static <T> void runEffect(BindChildrenEffectContext<T> context) {
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

    private void close() {
        elementEffect.close();
        elementEffect = null;
    }

    /**
     * Validate that parent element has no children not belonging to the list of
     * child signals.
     */
    private static <T> void validate(BindChildrenEffectContext<T> context) {
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
    private static <T> void removeNotPresentChildren(
            BindChildrenEffectContext<T> context,
            HashSet<Element> remainingChildrenSet) {
        var toRemove = new HashSet<>(context.valueSignalToChildCache.keySet());
        context.childSignalsList.forEach(toRemove::remove);
        for (ValueSignal<T> removedItem : toRemove) {
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
    private static <T> void updateByChildSignals(
            BindChildrenEffectContext<T> context,
            LinkedList<Element> remainingChildren,
            HashSet<Element> remainingChildrenSet) {

        for (int i = 0; i < context.childSignalsList.size(); i++) {
            ValueSignal<T> item = context.childSignalsList.get(i);

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
     * Record for
     * {@link #bindChildren(Component, ListSignal, SerializableFunction)} effect
     * to update children of a parent element according to a list of child
     * signals.
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
     */
    private record BindChildrenEffectContext<T>(Element parentElement,
            List<ValueSignal<T>> childSignalsList,
            SerializableFunction<ValueSignal<T>, Element> childElementFactory,
            HashMap<ValueSignal<T>, Element> valueSignalToChildCache)
            implements
                Serializable {

        /**
         * Return existing element or generate new by child element factory.
         *
         * @throws IllegalStateException
         *             if child factory adds or removes unexpected child
         */
        private Element getElement(ValueSignal<T> item) {
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
