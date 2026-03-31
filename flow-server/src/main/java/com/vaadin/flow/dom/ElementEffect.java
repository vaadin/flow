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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableExecutor;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.DeniedSignalUsageException;
import com.vaadin.flow.signals.EffectContext;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalEnvironment;
import com.vaadin.flow.signals.function.ContextualEffectAction;
import com.vaadin.flow.signals.function.EffectAction;
import com.vaadin.flow.signals.impl.Effect;
import com.vaadin.flow.signals.impl.UsageTracker;

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
    private final ContextualEffectAction effectFunction;
    private final Element owner;
    private Effect effect = null;
    private Registration attachRegistration;
    private Registration detachRegistration;
    /**
     * Error handler used by the active effect action. {@code null} means
     * exceptions are re-thrown (probe / unattached mode).
     */
    private @Nullable SerializableBiConsumer<Exception, Element> errorHandler = null;

    public ElementEffect(Element owner, EffectAction effectFunction) {
        this(owner, (ContextualEffectAction) ctx -> effectFunction.execute());
    }

    public ElementEffect(Element owner, ContextualEffectAction effectFunction) {
        Objects.requireNonNull(owner, "Owner element cannot be null");
        Objects.requireNonNull(effectFunction,
                "Effect function cannot be null");
        this.effectFunction = effectFunction;
        this.owner = owner;

        if (owner.getNode().isAttached()) {
            // Element is already attached: set up the error handler and
            // UI-locked dispatcher before creating the Effect so that the
            // initial (synchronous) run uses the proper error-routing and
            // execution context.
            enableEffect(owner);

            detachRegistration = owner.addDetachListener(detach -> {
                disableEffect();
                detachRegistration.remove();
                detachRegistration = null;
            });
        } else {
            // Element is not yet attached: run a probe immediately so that
            // structural errors (e.g. MissingSignalUsageException) are reported
            // at the call site rather than delayed until attach. The probe uses
            // Runnable::run so the first revalidation is synchronous. The
            // effect is then passivated so it does not actively listen for
            // changes while the element is detached.
            effect = new Effect(this::executeAction, Runnable::run);
            effect.passivate();
        }

        attachRegistration = owner.addAttachListener(attach -> {
            enableEffect(attach.getSource());

            detachRegistration = owner.addDetachListener(detach -> {
                disableEffect();
                detachRegistration.remove();
                detachRegistration = null;
            });
        });
    }

    /**
     * Executes the effect function, routing exceptions through the
     * {@link #errorHandler} when attached (active mode) or re-throwing them
     * when no error handler is set (probe/unattached mode). This is a named
     * method rather than a lambda to ensure reliable serialization.
     */
    private void executeAction(EffectContext ctx) {
        try {
            effectFunction.execute(ctx);
        } catch (DeniedSignalUsageException e) {
            // Programming error: signal.get() used in wrong context
            // (e.g. inside bindChildren factory). Always propagate so
            // the caller gets an immediate exception.
            throw e;
        } catch (RuntimeException e) {
            SerializableBiConsumer<Exception, Element> handler = errorHandler;
            if (handler != null) {
                handler.accept(e, owner);
            } else {
                // Probe run: re-throw so the exception surfaces at the
                // call site (e.g. inside bindText / Signal.effect).
                throw e;
            }
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
            EffectAction effectFunction) {
        ElementEffect effect = new ElementEffect(owner, effectFunction);
        return effect::close;
    }

    /**
     * Creates a context-aware Signal effect that is owned by a given element.
     * The effect is enabled when the element is attached and automatically
     * disabled when it is detached. The effect action receives an
     * {@link EffectContext} providing information about why the effect is
     * running, allowing the callback to distinguish between the initial
     * execution, updates triggered by the effect owner's requests, and updates
     * triggered by background changes (such as a background thread or another
     * user modifying a shared signal).
     * <p>
     * Example of usage:
     *
     * <pre>
     * Registration effect = ElementEffect.effect(myElement, ctx -&gt; {
     *     span.setText("$" + priceSignal.get());
     *     if (ctx.isBackgroundChange()) {
     *         span.getElement().executeJs("this.classList.add('highlight')");
     *     }
     * });
     * effect.remove(); // to remove the effect when no longer needed
     * </pre>
     *
     * @see Signal#unboundEffect(EffectAction)
     * @see EffectContext#isInitialRun()
     * @see EffectContext#isBackgroundChange()
     * @param owner
     *            the owner element for which the effect is applied, must not be
     *            <code>null</code>
     * @param effectFunction
     *            the context-aware effect function to be executed when any
     *            dependency is changed, receiving an {@link EffectContext} with
     *            information about the trigger, must not be <code>null</code>
     * @return a {@link Registration} that can be used to remove the effect
     *         function
     */
    public static Registration effect(Element owner,
            ContextualEffectAction effectFunction) {
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
     * SignalBinding&lt;String&gt; binding = ElementEffect.bind(mySpan, stringSignal,
     *         Element::setText);
     *
     * binding.onChange(ctx -&gt; {
     *     if (ctx.isBackgroundChange()) {
     *         ctx.getElement().flashClass("highlight");
     *     }
     * });
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
     * @return a {@link SignalBinding} that can be used to register change
     *         callbacks
     * @param <T>
     *            the type of the signal value
     */
    public static <T extends @Nullable Object> SignalBinding<T> bind(
            Element owner, Signal<T> signal,
            SerializableBiConsumer<Element, T> setter) {
        SignalBinding<T> binding = new SignalBinding<>();
        new ElementEffect(owner, new ContextualEffectAction() {
            private T previousValue;
            private boolean hasRun = false;

            @Override
            public void execute(EffectContext ctx) {
                T newValue = signal.get();
                T oldValue = hasRun ? previousValue : newValue;
                setter.accept(owner, newValue);
                if (ctx.isInitialRun() || binding.hasCallbacks()) {
                    var bindingContext = new BindingContext<>(
                            ctx.isInitialRun(), ctx.isBackgroundChange(),
                            oldValue, newValue, owner);
                    binding.setInitialContext(bindingContext);
                    if (binding.hasCallbacks()) {
                        binding.fireOnChange(bindingContext);
                    }
                }

                previousValue = newValue;
                hasRun = true;
            }
        });
        return binding;
    }

    private void enableEffect(Element owner) {
        Component parentComponent = ComponentUtil.findParentComponent(owner)
                .get();
        UI ui = parentComponent.getUI().get();

        // Install the UI error handler so that exceptions during active
        // (post-attach) runs are routed to the session error handler instead
        // of being re-thrown.
        errorHandler = (e, elem) -> ui.getSession().getErrorHandler()
                .error(new ErrorEvent(e, elem.getNode()));

        SerializableExecutor uiDispatcher = command -> {
            if (UI.getCurrent() == ui) {
                // Run immediately if on the same UI
                command.run();
            } else {
                SignalEnvironment.getDefaultEffectDispatcher().execute(() -> {
                    try {
                        // Guard against detach while waiting for lock
                        if (effect != null) {
                            // In test environments, the effect dispatcher
                            // may run tasks on the test thread where
                            // UI.getCurrent() is already set. In that case
                            // run directly instead of ui.access() which
                            // would only enqueue without executing.
                            if (UI.getCurrent() == ui) {
                                command.run();
                            } else {
                                ui.access(command::run);
                            }
                        }
                    } catch (UIDetachedException e) {
                        // Effect was concurrently disabled -> nothing do to
                    }
                });
            }
        };

        if (effect == null) {
            // First attach for the already-attached path (effect not yet
            // created): create the Effect directly with the UI dispatcher so
            // that the initial run uses the proper execution context.
            effect = new Effect(this::executeAction, uiDispatcher);
        } else {
            // Re-attach after detach (or the not-attached probe path):
            // swap the dispatcher and activate. activate() will only re-run
            // the callback if a signal changed while the element was detached.
            effect.setDispatcher(uiDispatcher);
            effect.activate();
        }
        effect.setOwnerUI(ui);
    }

    private void disableEffect() {
        if (effect != null) {
            effect.passivate();
        }
    }

    public void close() {
        if (effect != null) {
            effect.dispose();
            effect = null;
        }
        if (attachRegistration != null) {
            attachRegistration.remove();
            attachRegistration = null;
        }
        if (detachRegistration != null) {
            detachRegistration.remove();
            detachRegistration = null;
        }
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
     * The parent element must not contain any children in the default slot
     * (i.e. without a {@code slot} attribute) that are not part of the list
     * signal. If the parent has existing default-slot children when this method
     * is called, or if it contains unrelated default-slot children after the
     * list changes, an {@link IllegalStateException} will be thrown. Named-slot
     * children are allowed and will be preserved. The child factory must not
     * produce elements with a {@code slot} attribute.
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
     *             thrown if parent element has default-slot children, or if the
     *             child factory produces elements with a {@code slot} attribute
     */
    public static <T extends @Nullable Object, S extends Signal<T>> Registration bindChildren(
            Element parentElement, Signal<List<S>> list,
            SerializableFunction<S, Element> childFactory) {
        Objects.requireNonNull(parentElement, "Parent element cannot be null");
        Objects.requireNonNull(parentElement, "Parent element cannot be null");
        Objects.requireNonNull(list, "List signal cannot be null");
        Objects.requireNonNull(childFactory,
                "Child element factory cannot be null");

        boolean hasDefaultSlotChildren = parentElement.getChildren()
                .anyMatch(child -> child.getAttribute("slot") == null);
        if (hasDefaultSlotChildren) {
            throw new IllegalStateException(
                    "Parent element must not have children in the default slot when binding a list signal to it");
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

    private static <T extends @Nullable Object, S extends Signal<T>> void runEffect(
            BindChildrenEffectContext<T, S> context) {
        // Cache the children to avoid multiple traversals
        LinkedList<Element> remainingChildren = context
                .parentDefaultSlotChildrenList();
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
    private static <T extends @Nullable Object, S extends Signal<T>> void validate(
            BindChildrenEffectContext<T, S> context) {
        LinkedList<Element> children = context.parentDefaultSlotChildrenList();
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
    private static <T extends @Nullable Object, S extends Signal<T>> void removeNotPresentChildren(
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
    private static <T extends @Nullable Object, S extends Signal<T>> void updateByChildSignals(
            BindChildrenEffectContext<T, S> context,
            LinkedList<Element> remainingChildren,
            HashSet<Element> remainingChildrenSet) {

        for (int i = 0; i < context.childSignalsList.size(); i++) {
            S item = context.childSignalsList.get(i);

            Element expectedChild = context.getElement(item);
            if (remainingChildrenSet.isEmpty() || !Objects
                    .equals(expectedChild.getParent(), context.parentElement)) {
                context.parentElement.insertChild(context.toActualIndex(i),
                        expectedChild);
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
                    context.parentElement.insertChild(context.toActualIndex(i),
                            expectedChild);

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
    private record BindChildrenEffectContext<T extends @Nullable Object, S extends Signal<T>>(
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
            return valueSignalToChildCache.computeIfAbsent(item, signal -> {
                Element element = UsageTracker.track(
                        () -> childElementFactory.apply(signal), usage -> {
                            throw new DeniedSignalUsageException(
                                    "Detected Signal.get() call inside a "
                                            + "bindChildren child factory "
                                            + "callback. Use peek() to read "
                                            + "the value without setting up a "
                                            + "dependency, or pass the signal "
                                            + "to a component that creates its "
                                            + "own reactive binding "
                                            + "(e.g. new Span(() -> "
                                            + "signal.get())).");
                        });
                if (element.getAttribute("slot") != null) {
                    throw new IllegalStateException(
                            "Children created by the bindChildren factory must not have a slot attribute set");
                }
                return element;
            });
        }

        /**
         * Returns size of the <code>valueSignalToChildCache</code> map holding
         * cached child elements by value signal.
         */
        private int getCachedChildrenSize() {
            return valueSignalToChildCache.size();
        }

        /**
         * Translates a logical index among default-slot children to the actual
         * DOM child index, skipping over slotted children.
         */
        private int toActualIndex(int defaultSlotIndex) {
            int actualIndex = 0;
            int defaultSlotCount = 0;
            int totalChildren = parentElement.getChildCount();
            while (actualIndex < totalChildren
                    && defaultSlotCount < defaultSlotIndex) {
                if (parentElement.getChild(actualIndex)
                        .getAttribute("slot") == null) {
                    defaultSlotCount++;
                }
                actualIndex++;
            }
            return actualIndex;
        }

        private LinkedList<Element> parentDefaultSlotChildrenList() {
            return parentElement.getChildren()
                    .filter(child -> child.getAttribute("slot") == null)
                    .collect(Collectors.toCollection(LinkedList::new));
        }
    }
}
