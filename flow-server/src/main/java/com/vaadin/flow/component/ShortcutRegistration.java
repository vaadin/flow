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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.vaadin.flow.component.internal.ShortcutFilterJs;
import com.vaadin.flow.component.internal.ShortcutJs;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.shared.Registration;

/**
 * A registration object for both configuring and removing the registered
 * keyboard shortcut.
 *
 * @author Vaadin Ltd.
 * @since 1.3
 */
public class ShortcutRegistration implements Registration, Serializable {
    static final String LISTEN_ON_COMPONENTS_SHOULD_NOT_CONTAIN_NULL = "listenOnComponents should not contain null!";
    static final String LISTEN_ON_COMPONENTS_SHOULD_NOT_HAVE_DUPLICATE_ENTRIES = "listenOnComponents should not have duplicate entries!";

    private static final ShortcutFilterJs FILTERS = JsSnippets
            .of(ShortcutFilterJs.class);

    private boolean allowDefaultBehavior = false;
    private boolean allowEventPropagation = false;

    private boolean allowEventsFromNestedModals = false;

    // Marker attribute set on the lifecycle owner element so the client-side
    // origin guard can locate it within the event's popover/modal scope
    // (#24974). The token is a UUID rather than a JVM-local counter so it stays
    // unique across session serialization/deserialization onto another JVM,
    // where a counter would restart and new shortcuts could reuse a restored
    // token.
    static final String SHORTCUT_OWNER_ATTRIBUTE = "data-vaadin-shortcut-owner";
    private String ownerToken;

    private boolean resetFocusOnActiveElement = false;

    static final String LISTEN_ON_INITIALIZED = "_initialized_listen_on_for_component";

    private final Set<Key> modifiers = HashSet.newHashSet(2);
    private Key primaryKey = null;

    private StateTree.ExecutionRegistration executionRegistration;
    // lifecycle owner
    // usually lifecycleRegistration == listenerRegistration
    private CompoundRegistration lifecycleRegistration;
    private Component lifecycleOwner;
    // event listener owner
    private CompoundRegistration[] listenOnAttachListenerRegistrations;
    private CompoundRegistration[] shortcutListenerRegistrations;
    private Component[] listenOnComponents;

    private boolean shortcutActive = false;

    private SerializableSupplier<Component[]> listenOnSuppliers;

    // used to determine, if we need to do something before client response
    private final AtomicBoolean isDirty = new AtomicBoolean(false);

    private ShortcutEventListener eventListener;

    private final List<Registration> registrations = new ArrayList<>();

    private DisabledUpdateMode mode = DisabledUpdateMode.ONLY_WHEN_ENABLED;

    // beforeClientResponse callback
    // needs to be an anonymous class to prevent deserialization issues
    // see #17201
    private final SerializableConsumer<ExecutionContext> beforeClientResponseConsumer = new SerializableConsumer<>() {
        @Override
        public void accept(ExecutionContext executionContext) {

            if (listenOnComponents == null) {
                initListenOnComponent();
            }
            addListenOnDetachListeners();

            boolean reinit = false;
            /*
             * In PreserveOnRefersh case the UI instance is not detached
             * immediately (once detach happens the initialization is rerun in
             * initListenOnComponent via removeAllListenerRegistrations), so we
             * may not rely on detach event only: the check whether UI is
             * already marked as closing is done here which should rerun the
             * initialization immediately.
             */
            for (Component component : listenOnComponents) {
                if (component.getUI().map(UI::isClosing).orElse(false)) {
                    reinit = true;
                    break;
                }
            }
            if (reinit) {
                removeAllListenerRegistrations();
                initListenOnComponent();
                addListenOnDetachListeners();
            }

            for (int i = 0; i < listenOnComponents.length; i++) {
                updateHandlerListenerRegistration(i);
            }

            updateOwnerMarker();

            markClean();
        }
    };

    /**
     * @param lifecycleOwner
     *            This is the component which controls when the shortcut is
     *            actually active. If the component is either detached or
     *            invisible, the shortcut will not be active
     * @param listenOnSuppliers
     *            Suppliers for components to which the shortcut listeners are
     *            bound to. Suppliers are given in order to get around some
     *            cases where the components might not be immediately available.
     *            Must not be null.
     * @param eventListener
     *            The listener to invoke when the shortcut detected
     * @param key
     *            Primary key of the shortcut. This can not be a
     *            {@link KeyModifier}.
     */
    ShortcutRegistration(Component lifecycleOwner,
            SerializableSupplier<Component[]> listenOnSuppliers,
            ShortcutEventListener eventListener, Key key) {
        if (Key.isModifier(key)) {
            throw new IllegalArgumentException(
                    String.format("Parameter " + "'key' cannot belong to %s",
                            KeyModifier.class.getSimpleName()));
        }
        Objects.requireNonNull(listenOnSuppliers,
                "listenOnSuppliers may not be null!");

        this.eventListener = eventListener;
        this.listenOnSuppliers = listenOnSuppliers;
        setLifecycleOwner(lifecycleOwner);

        // addKey cannot be called without lifecycleOwner
        addKey(key);
    }

    /**
     * Configures {@link KeyModifier KeyModifiers} for the shortcut. Calling
     * this method will overwrite any previously set modifier keys. Hence,
     * calling {@code shortcutRegistration.withModifiers();} will remove all
     * previously set modifier keys.
     *
     * @param keyModifiers
     *            Key modifiers. Can be empty.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withModifiers(KeyModifier... keyModifiers) {
        this.modifiers.clear();
        prepareForClientResponse();
        for (KeyModifier keyModifier : keyModifiers) {
            addKey(keyModifier);
        }
        return this;
    }

    /**
     * Fluently adds {@link KeyModifier#ALT} to the shortcut's modifiers.
     *
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withAlt() {
        addKey(KeyModifier.ALT);
        return this;
    }

    /**
     * Fluently adds {@link KeyModifier#CONTROL} to the shortcut's modifiers.
     *
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withCtrl() {
        addKey(KeyModifier.CONTROL);
        return this;
    }

    /**
     * Fluently adds {@link KeyModifier#META} to the shortcut's modifiers.
     *
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withMeta() {
        addKey(KeyModifier.META);
        return this;
    }

    /**
     * Fluently adds {@link KeyModifier#SHIFT} to the shortcut's modifiers.
     *
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withShift() {
        addKey(KeyModifier.SHIFT);
        return this;
    }

    /**
     * Allows the default keyboard event handling when the shortcut is invoked.
     *
     * @return this <code>ShortcutRegistration</code>
     * @see #setBrowserDefaultAllowed(boolean)
     */
    public ShortcutRegistration allowBrowserDefault() {
        if (!allowDefaultBehavior) {
            allowDefaultBehavior = true;
            prepareForClientResponse();
        }
        return this;
    }

    /**
     * Allow the event to propagate upwards in the DOM tree, when the shortcut
     * is invoked.
     *
     * @return this <code>ShortcutRegistration</code>
     * @see #setEventPropagationAllowed(boolean)
     */
    public ShortcutRegistration allowEventPropagation() {
        if (!allowEventPropagation) {
            allowEventPropagation = true;
            prepareForClientResponse();
        }
        return this;
    }

    /**
     * Allow the shortcut to trigger for keyboard events that originate from a
     * modal or popover nested inside the shortcut's listening scope.
     * <p>
     * By default the shortcut does not trigger for such events. This restores
     * behavior where a shortcut bound to a parent overlay would react to a
     * keydown coming from a nested dialog/overlay that has focus.
     *
     * @return this <code>ShortcutRegistration</code>
     * @see #setEventsFromNestedModalsAllowed(boolean)
     * @since 25.2.6
     */
    public ShortcutRegistration allowEventsFromNestedModals() {
        if (!allowEventsFromNestedModals) {
            allowEventsFromNestedModals = true;
            prepareForClientResponse();
        }
        return this;
    }

    /**
     * Reset focus on active element before triggering shortcut event handler.
     *
     * @return this <code>ShortcutRegistration</code>
     * @see #setResetFocusOnActiveElement(boolean)
     * @since 24.3
     */
    public ShortcutRegistration resetFocusOnActiveElement() {
        if (!resetFocusOnActiveElement) {
            resetFocusOnActiveElement = true;
            prepareForClientResponse();
        }
        return this;
    }

    /**
     * Binds the shortcut's life cycle to that of the given {@link Component}.
     * When the given {@code component} is attached, the shortcut's listener is
     * attached to the {@code Component} that owns the shortcut. When the given
     * {@code component} is detached, so is the listener. is detached, the
     * shortcut is removed from all attached scopes.
     *
     * @param component
     *            New lifecycle owner of the shortcut
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration bindLifecycleTo(Component component) {
        // Changing the lifecycle owner moves the owner marker the origin guard
        // relies on, so setLifecycleOwner re-runs the client update when the
        // owner actually changes.

        if (component == null) {
            throw new IllegalArgumentException(
                    String.format(Shortcuts.NULL, "component"));
        }

        setLifecycleOwner(component);
        return this;
    }

    /**
     * Fluently define the {@link Component} onto which the shortcut's listener
     * is bound. Calling this method will remove the previous listener from the
     * {@code component} it was bound to.
     *
     * @param listenOnComponents
     *            {@code Component}s onto which the shortcut listeners are
     *            bound. Must not be null. Must not contain null. Must not have
     *            duplicate components.
     * @return this <code>ShortcutRegistration</code>
     * @since 2.2
     */
    public ShortcutRegistration listenOn(Component... listenOnComponents) {
        Objects.requireNonNull(listenOnComponents,
                "listenOnComponents must not be null!");
        if (Arrays.stream(listenOnComponents).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    LISTEN_ON_COMPONENTS_SHOULD_NOT_CONTAIN_NULL);
        }
        if (hasDuplicate(listenOnComponents)) {
            throw new IllegalArgumentException(
                    LISTEN_ON_COMPONENTS_SHOULD_NOT_HAVE_DUPLICATE_ENTRIES);
        }
        removeAllListenerRegistrations();
        final Component[] copyOfListenOnComponents = Arrays
                .copyOf(listenOnComponents, listenOnComponents.length);
        this.listenOnSuppliers = () -> copyOfListenOnComponents;
        prepareForClientResponse();

        return this;
    }

    private boolean hasDuplicate(Component[] components) {
        Set<Component> set = new HashSet<>(components.length);
        for (Component component : components) {
            if (!set.add(component)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes the {@code ShortcutRegistration}
     * <p>
     * Removes all the underlying registrations tied to owner and lifecycle
     * owner {@link Component components}.
     */
    @Override
    public void remove() {
        if (executionRegistration != null) {
            executionRegistration.remove();
            executionRegistration = null;
        }
        if (lifecycleRegistration != null) {
            lifecycleRegistration.remove();
            lifecycleRegistration = null;
        }
        removeAllListenerRegistrations();

        if (lifecycleOwner != null && lifecycleOwner.getElement() != null) {
            removeOwnerToken(lifecycleOwner.getElement());
        }
        lifecycleOwner = null;
        listenOnComponents = null;

        eventListener = null;
    }

    /**
     * Is the shortcut active on the current UI. For this to be true, the
     * lifecycle owner needs to be attached and visible and handler owner needs
     * to be attached.
     *
     * @return Is the shortcut active
     */
    public boolean isShortcutActive() {
        return shortcutActive;
    }

    /**
     * Get the primary {@link Key} of the shortcut. Primary key can be any key
     * besides modifier keys.
     *
     * @return Primary key
     */
    public Key getKey() {
        return primaryKey;
    }

    /**
     * Get a set of {@link Key keys} where each {@code key} is an instance of a
     * {@link KeyModifier}.
     *
     * @return Set of modifier keys
     */
    public Set<Key> getModifiers() {
        return Collections.unmodifiableSet(modifiers);
    }

    /**
     * Checks if the default key behaviour in the browser is allowed by the
     * shortcut. The default value is {@code false}.
     *
     * @return Allows default key behavior
     * @since 1.4
     */
    public boolean isBrowserDefaultAllowed() {
        return allowDefaultBehavior;
    }

    /**
     * Set whether the default key behavior is allowed in the browser. The
     * default value is {@code false}, and it prevents the default key events
     * from taking place in the browser.
     *
     * @param browserDefaultAllowed
     *            Allow default behavior on keydown
     * @since 1.4
     */
    public void setBrowserDefaultAllowed(boolean browserDefaultAllowed) {
        if (allowDefaultBehavior != browserDefaultAllowed) {
            allowDefaultBehavior = browserDefaultAllowed;
            prepareForClientResponse();
        }
    }

    /**
     * Checks if the shortcut allows keydown event (associated with the
     * shortcut) propagation in the browser. The default value is {@code false}.
     *
     * @return Allows event propagation
     * @since 1.4
     */
    public boolean isEventPropagationAllowed() {
        return allowEventPropagation;
    }

    /**
     * Set whether shortcut's keydown event is allowed to propagate up the DOM
     * tree in the browser. The default value is {@code false}, and the DOM
     * event is consumed by the shortcut handler.
     *
     * @param eventPropagationAllowed
     *            Allow event propagation
     * @since 1.4
     */
    public void setEventPropagationAllowed(boolean eventPropagationAllowed) {
        if (allowEventPropagation != eventPropagationAllowed) {
            allowEventPropagation = eventPropagationAllowed;
            prepareForClientResponse();
        }
    }

    /**
     * Checks if the shortcut triggers for keyboard events originating from a
     * modal or popover nested inside the shortcut's listening scope. The
     * default value is {@code false}.
     *
     * @return {@code true} if events from nested modals are allowed to trigger
     *         the shortcut
     * @since 25.2.6
     */
    public boolean isEventsFromNestedModalsAllowed() {
        return allowEventsFromNestedModals;
    }

    /**
     * Sets whether the shortcut triggers for keyboard events that originate
     * from a modal or popover nested inside the shortcut's listening scope. The
     * default value is {@code false}, meaning a keydown coming from a nested
     * dialog/overlay does not trigger a shortcut bound to a parent scope.
     *
     * @param eventsFromNestedModalsAllowed
     *            {@code true} to allow events from nested modals to trigger the
     *            shortcut
     * @since 25.2.6
     */
    public void setEventsFromNestedModalsAllowed(
            boolean eventsFromNestedModalsAllowed) {
        if (allowEventsFromNestedModals != eventsFromNestedModalsAllowed) {
            allowEventsFromNestedModals = eventsFromNestedModalsAllowed;
            prepareForClientResponse();
        }
    }

    /**
     * Checks if shortcut resets focus on active element before triggering
     * shortcut event handler.
     *
     * @return True when focus is reset on the active element, false otherwise.
     * @since 24.3
     */
    public boolean isResetFocusOnActiveElement() {
        return resetFocusOnActiveElement;
    }

    /**
     * Set if shortcut resets focus on active element before triggering shortcut
     * event handler. Reset means to first <code>blur()</code> and then
     * <code>focus()</code> the element. If element is an input field with a
     * ValueChangeMode.ON_CHANGE, then resetting focus ensures that value change
     * event is triggered before shortcut event is handled. Active element is
     * <code>document.activeElement</code> or
     * <code>shadowRoot.activeElement</code> if active element is the
     * <code>shadowRoot</code>.
     *
     * @param resetFocusOnActiveElement
     *            Reset focus on active element
     * @since 24.3
     */
    public void setResetFocusOnActiveElement(
            boolean resetFocusOnActiveElement) {
        if (resetFocusOnActiveElement != this.resetFocusOnActiveElement) {
            this.resetFocusOnActiveElement = resetFocusOnActiveElement;
            prepareForClientResponse();
        }
    }

    /**
     * The {@link Component}s which own the shortcuts key event listeners.
     *
     * @return Component[]
     * @since 2.2
     */
    public Component[] getOwners() {
        if (listenOnComponents == null) {
            return new Component[0];
        }
        return listenOnComponents;
    }

    /**
     * {@link Component} which controls when the shortcut is active and when it
     * is not.
     *
     * @return Component
     * @see #isShortcutActive()
     */
    public Component getLifecycleOwner() {
        return lifecycleOwner;
    }

    /**
     * Configure whether this listener will be called even in cases when the
     * component is disabled. Defaults to
     * {@link DisabledUpdateMode#ONLY_WHEN_ENABLED}.
     *
     * @param disabledUpdateMode
     *            {@link DisabledUpdateMode#ONLY_WHEN_ENABLED} to only fire
     *            events when the component is enabled,
     *            {@link DisabledUpdateMode#ALWAYS} to fire events also when the
     *            component is disabled.
     *
     * @return this registration, for chaining
     * @since 24.7
     */
    public ShortcutRegistration setDisabledUpdateMode(
            DisabledUpdateMode disabledUpdateMode) {
        if (disabledUpdateMode == null) {
            throw new IllegalArgumentException(
                    "RPC communication control mode for disabled element must not be null");
        }
        mode = disabledUpdateMode;
        return this;
    }

    /**
     * Returns whether this listener will be called even in cases when the
     * component is disabled.
     *
     * @return current disabledUpdateMode for this listener
     * @since 24.7
     */
    public DisabledUpdateMode getDisabledUpdateMode() {
        return mode;
    }

    /**
     * Used for testing purposes.
     *
     * @return Is there a need to write shortcut changes to the client
     */
    boolean isDirty() {
        return isDirty.get();
    }

    private void setLifecycleOwner(Component owner) {
        assert owner != null;

        if (lifecycleRegistration != null) {
            lifecycleRegistration.remove();
        }

        final boolean ownerChanged = lifecycleOwner != null
                && lifecycleOwner != owner;
        if (ownerChanged && lifecycleOwner.getElement() != null) {
            // Drop the marker from the previous owner; the new owner is marked
            // by updateOwnerMarker on the client update triggered below.
            removeOwnerToken(lifecycleOwner.getElement());
        }

        registerLifecycleOwner(owner);

        if (ownerChanged) {
            prepareForClientResponse();
        }
    }

    private void addKey(Key key) {
        assert key != null;

        HashableKey hashableKey = new HashableKey(key);

        if (Key.isModifier(key)) {
            if (!modifiers.contains(hashableKey)) {
                modifiers.add(hashableKey);
                prepareForClientResponse();
            }
        } else {
            if (primaryKey == null || !primaryKey.equals(hashableKey)) {
                primaryKey = hashableKey;
                prepareForClientResponse();
            }
        }
    }

    private void prepareForClientResponse() {
        assert lifecycleOwner != null;

        synchronized (this) {
            if (isDirty.get()) {
                return;
            }
            isDirty.set(true);
        }

        // if lifecycleOwner is attached, we'll register new
        // beforeClientResponse callback. Otherwise we'll need to wait for the
        // lifecycleOwner's attach-callback to do it
        queueBeforeExecutionCallback();
    }

    private void markClean() {
        synchronized (this) {
            if (!isDirty.get()) {
                return;
            }
            isDirty.set(false);
            executionRegistration = null;
        }
    }

    private String[] getKeyValues() {
        return primaryKey.getKeys().toArray(String[]::new);
    }

    private void updateHandlerListenerRegistration(int listenOnIndex) {
        final Component component = getComponentEventSource(listenOnIndex);

        if (shortcutListenerRegistrations == null) {
            shortcutListenerRegistrations = new CompoundRegistration[listenOnComponents.length];
        }

        if (shortcutListenerRegistrations[listenOnIndex] == null) {
            if (component.getUI().isPresent()) {
                shortcutListenerRegistrations[listenOnIndex] = new CompoundRegistration();
                Registration keyDownRegistration = ComponentUtil.addListener(
                        component, KeyDownEvent.class,
                        event -> fireShortcutEvent(component),
                        domRegistration -> {
                            shortcutListenerRegistrations[listenOnIndex]
                                    .addRegistration(domRegistration);
                            configureHandlerListenerRegistration(listenOnIndex);
                        });
                shortcutListenerRegistrations[listenOnIndex]
                        .addRegistration(keyDownRegistration);
                setupKeydownEventDelegateIfNeeded(component);
            }
        } else {
            configureHandlerListenerRegistration(listenOnIndex);
        }
    }

    private Component getComponentEventSource(int listenOnIndex) {
        Component component = listenOnComponents[listenOnIndex];
        assert component != null;

        if (component instanceof UI) {
            UIInternals uiInternals = ((UI) component).getInternals();
            // Shortcut events go to UI by default, which will be inert from
            // receiving RPCs if there is an active modal component. In this
            // case, check if the owner component is a child of the modal
            // component, and if so, send via the modal component.
            if (uiInternals.hasModalComponent()) {
                Optional<Component> maybeAncestor = Optional.of(lifecycleOwner);
                while (maybeAncestor.isPresent()) {
                    Component ancestor = maybeAncestor.get();
                    if (ancestor == uiInternals.getActiveModalComponent()) {
                        return ancestor;
                    }
                    maybeAncestor = ancestor.getParent();
                }
            }
        }
        return component;
    }

    private void fireShortcutEvent(Component component) {
        if (lifecycleOwner == null) {
            return;
        }
        if (ComponentUtil.isEffectivelyVisible(lifecycleOwner)
                && (lifecycleOwner.getElement().isEnabled()
                        || DisabledUpdateMode.ALWAYS
                                .equals(getDisabledUpdateMode()))) {
            invokeShortcutEventListener(component);
        }
    }

    private void configureHandlerListenerRegistration(int listenOnIndex) {
        if (shortcutListenerRegistrations[listenOnIndex] != null) {
            Optional<Registration> registration = shortcutListenerRegistrations[listenOnIndex].registrations
                    .stream().filter(r -> r instanceof DomListenerRegistration)
                    .findFirst();

            registration.ifPresent(r -> {
                DomListenerRegistration listenerRegistration = (DomListenerRegistration) r;

                /*
                 * Ignore keydown events that originate from a modal/popover
                 * nested deeper than the shortcut's lifecycle owner (#24974).
                 * The listener is often bound to the UI (body) while the owner
                 * lives inside an overlay, so the guard is anchored on the
                 * owner's own popover/modal scope, not the listener element. It
                 * runs before the preventDefault/stopPropagation side effects
                 * so that non-matching events are left untouched.
                 *
                 * Skipped when this listenOn has an element-locator delegate:
                 * that delegate evaluates its own boundary guard on the
                 * original event and then re-dispatches a cloned event to
                 * listenOn. The clone's composedPath no longer reflects the
                 * real origin, so the composedPath-based owner-scope guard must
                 * not run on it.
                 */
                final boolean hasEventDelegate = ComponentUtil.getData(
                        getComponentEventSource(listenOnIndex),
                        Shortcuts.ELEMENT_LOCATOR_JS_KEY) != null;
                final boolean checkOwnerScope = !allowEventsFromNestedModals
                        && !hasEventDelegate;
                /*
                 * The filter itself prevents the default and stops the
                 * propagation of the events that pass it, rather than
                 * preventDefault() and stopPropagation() of the registration:
                 * those add event data that cannot be removed again when the
                 * options of the shortcut change.
                 */
                DomEventFilter filter = FILTERS.matchKeydown(
                        getModifierNames(modifiers, true),
                        getModifierNames(modifiers, false), checkOwnerScope,
                        checkOwnerScope ? getOwnerSelector() : null,
                        !allowDefaultBehavior, !allowEventPropagation,
                        resetFocusOnActiveElement, getKeyValues());
                listenerRegistration.setFilterExpression(filter);
                shortcutActive = true;
            });
        }
    }

    private void invokeShortcutEventListener(Component component) {
        // construct the event
        final ShortcutEvent event = new ShortcutEvent(component, lifecycleOwner,
                primaryKey,
                modifiers.stream().map(k -> (KeyModifier) ((HashableKey) k).key)
                        .collect(Collectors.toSet()));

        eventListener.onShortcut(event);
    }

    private void registerLifecycleOwner(Component owner) {
        assert owner != null;

        lifecycleOwner = owner;

        // since we are attached, UI should be available
        Registration attachRegistration = owner
                .addAttachListener(e -> queueBeforeExecutionCallback());
        // remove shortcut listener when detached
        Registration detachRegistration = owner.addDetachListener(e -> {
            removeLifecycleOwnerRegistrations();
            if (executionRegistration != null) {
                executionRegistration.remove();
            }
        });

        lifecycleRegistration = new CompoundRegistration(attachRegistration,
                detachRegistration);
    }

    private Component[] registerOwnerListeners() {
        assert listenOnSuppliers != null;

        listenOnComponents = listenOnSuppliers.get();

        if (listenOnComponents == null) {
            throw new IllegalStateException(String.format(
                    "Could not register shortcut listener for %s. "
                            + "%s<%s> supplied a null value.",
                    this.toString(), SerializableSupplier.class.getSimpleName(),
                    Component.class.getSimpleName()));
        }

        listenOnAttachListenerRegistrations = new CompoundRegistration[listenOnComponents.length];
        for (int i = 0; i < listenOnComponents.length; i++) {
            Component component = listenOnComponents[i];
            final int listenOnIndex = i;
            if (component == null) {
                throw new IllegalStateException(String.format(
                        "Could not register shortcut listener for %s. "
                                + "%s<%s> supplied a null value.",
                        this.toString(),
                        SerializableSupplier.class.getSimpleName(),
                        Component.class.getSimpleName()));
            }
            if (!(component instanceof UI)) {
                listenOnAttachListenerRegistrations[i] = new CompoundRegistration();
                listenOnAttachListenerRegistrations[i]
                        .addRegistration(component.addAttachListener(
                                attachEvent -> updateHandlerListenerRegistration(
                                        listenOnIndex)));
                listenOnAttachListenerRegistrations[i]
                        .addRegistration(component.addDetachListener(
                                detachEvent -> removeLifecycleOwnerRegistrations()));
            }

            // either the scope is an active UI, or the component is attached to
            // an active UI - in either case, we want to update dom registration
            if (component.getUI().isPresent()) {
                updateHandlerListenerRegistration(listenOnIndex);
            }
        }
        return listenOnComponents;
    }

    private void removeAllListenerRegistrations() {
        if (listenOnAttachListenerRegistrations != null) {
            for (CompoundRegistration listenOnAttachListenerRegistration : listenOnAttachListenerRegistrations) {
                if (listenOnAttachListenerRegistration != null)
                    listenOnAttachListenerRegistration.remove();
            }
            listenOnAttachListenerRegistrations = null;
        }
        removeLifecycleOwnerRegistrations();
        listenOnComponents = null;
    }

    private void removeListenOnDetachListeners() {
        registrations.forEach(Registration::remove);
        registrations.clear();
    }

    private void removeLifecycleOwnerRegistrations() {
        if (shortcutListenerRegistrations != null) {
            for (CompoundRegistration shortcutListenerRegistration : shortcutListenerRegistrations) {
                if (shortcutListenerRegistration != null)
                    shortcutListenerRegistration.remove();
            }
            shortcutListenerRegistrations = null;
        }
        shortcutActive = false;
        removeListenOnDetachListeners();
    }

    private void queueBeforeExecutionCallback() {
        if (lifecycleOwner == null || !lifecycleOwner.getUI().isPresent()) {
            return;
        }

        if (executionRegistration != null) {
            executionRegistration.remove();
        }

        executionRegistration = lifecycleOwner.getUIOrThrow()
                .beforeClientResponse(lifecycleOwner,
                        beforeClientResponseConsumer);
    }

    /**
     * Gets the names of the modifiers that a keydown needs to have pressed, or
     * not pressed, to match the shortcut, as
     * {@code KeyboardEvent.getModifierState()} names them.
     * <p>
     * A modifier that is not part of the shortcut has to be released, so that
     * e.g. Ctrl+Shift+S does not trigger a Ctrl+S shortcut.
     *
     * @param modifiers
     *            the modifiers of the shortcut
     * @param pressed
     *            {@code true} for the modifiers that have to be pressed,
     *            {@code false} for those that must not be pressed
     * @return the modifier names separated by spaces, empty if there are none
     */
    private static String getModifierNames(Collection<Key> modifiers,
            boolean pressed) {

        final List<Key> realMods = modifiers.stream().filter(Key::isModifier)
                .collect(Collectors.toList());

        /*
         * Due to https://github.com/vaadin/flow/issues/15906. Browsers having
         * different implementation for Option keys: -> Mozilla: ALT_GRAPH and
         * ALT both are triggered (getModifierState() return true for ALT AND
         * ALT_GRAPH), -> Chrome: ALT is triggered only (getModifierState()
         * return true for only ALT).
         *
         * So we need to check if ALT is in the modifiers list, if it is, we do
         * not require ALT_GRAPH to be released, because it can be based on
         * browser implementations.
         */
        final boolean altAdded = modifiers.stream()
                .anyMatch(key -> key.matches(Key.ALT.getKeys().get(0)));

        return Arrays.stream(KeyModifier.values()).filter(modifier -> {
            String modKey = modifier.getKeys().get(0);
            boolean modifierRequired = realMods.stream()
                    .anyMatch(mod -> mod.matches(modKey));
            if (pressed) {
                return modifierRequired;
            }
            return !modifierRequired
                    && !(modifier == KeyModifier.ALT_GRAPH && altAdded);
        }).map(modifier -> modifier.getKeys().get(0))
                .collect(Collectors.joining(" "));
    }

    /**
     * Gets the selector that locates the lifecycle owner element on the client,
     * for the guard against events from a modal/popover nested deeper than the
     * owner. The guard fires only for an event that shares the popover/modal
     * scope of the owner element, which is located via its
     * {@link #SHORTCUT_OWNER_ATTRIBUTE} marker. An event outside any popover
     * fires; an event in a popover that does not contain the owner is treated
     * as originating from a foreign or nested scope and is ignored.
     *
     * @return the selector, or {@code null} when the owner is the UI
     */
    private String getOwnerSelector() {
        final String token = getOwnerToken();
        if (token == null) {
            // Only reached while the guard is on, so the owner is the UI, i.e.
            // <body>, which can never be inside an open popover/modal. Sharing
            // its scope therefore just means the event did not originate inside
            // one, which needs no owner token and so keeps the filter identical
            // for every UI-owned shortcut (#25624).
            return null;
        }
        return "[" + SHORTCUT_OWNER_ATTRIBUTE + "~=\"" + token + "\"]";
    }

    /**
     * The marker token of this registration, generated on first use.
     * <p>
     * A token is needed only when the owner element has to be marked for the
     * client-side origin guard to locate it, which is the case unless the guard
     * is off ({@link #allowEventsFromNestedModals}) or the lifecycle owner is
     * the UI, which the guard locates without a marker. Skipping the token
     * matters: it makes the listener filter unique per registration, and the
     * filter settings are kept for the lifetime of the UI by both constant
     * pools (#25624).
     *
     * @return the marker token, or {@code null} if none is needed
     */
    private String getOwnerToken() {
        if (allowEventsFromNestedModals || lifecycleOwner instanceof UI) {
            return null;
        }
        if (ownerToken == null) {
            ownerToken = "sc-" + UUID.randomUUID();
        }
        return ownerToken;
    }

    /**
     * Marks or unmarks the lifecycle owner element so that the selector from
     * {@link #getOwnerSelector()} can locate it on the client. The marker is
     * only needed while the origin guard is active.
     */
    private void updateOwnerMarker() {
        if (lifecycleOwner == null) {
            return;
        }
        final Element element = lifecycleOwner.getElement();
        if (element == null) {
            return;
        }
        final String token = getOwnerToken();
        if (token == null) {
            removeOwnerToken(element);
        } else {
            addOwnerToken(element, token);
        }
    }

    private void addOwnerToken(Element element, String token) {
        final String current = element.getAttribute(SHORTCUT_OWNER_ATTRIBUTE);
        if (current == null || current.isBlank()) {
            element.setAttribute(SHORTCUT_OWNER_ATTRIBUTE, token);
            return;
        }
        final Set<String> tokens = new LinkedHashSet<>(
                Arrays.asList(current.trim().split("\\s+")));
        if (tokens.add(token)) {
            element.setAttribute(SHORTCUT_OWNER_ATTRIBUTE,
                    String.join(" ", tokens));
        }
    }

    private void removeOwnerToken(Element element) {
        if (ownerToken == null) {
            // No token was ever created, so nothing was ever marked.
            return;
        }
        final String current = element.getAttribute(SHORTCUT_OWNER_ATTRIBUTE);
        if (current == null || current.isBlank()) {
            return;
        }
        final Set<String> tokens = new LinkedHashSet<>(
                Arrays.asList(current.trim().split("\\s+")));
        if (tokens.remove(ownerToken)) {
            if (tokens.isEmpty()) {
                element.removeAttribute(SHORTCUT_OWNER_ATTRIBUTE);
            } else {
                element.setAttribute(SHORTCUT_OWNER_ATTRIBUTE,
                        String.join(" ", tokens));
            }
        }
    }

    /*
     * #7799, vaadin/vaadin-dialog#229 This could be changed to use a generic
     * feature for DOM events by either using existing DOM event handling or by
     * using the JS execution return channel feature.
     */
    private void setupKeydownEventDelegateIfNeeded(Component listenOn) {
        final Object elementLocator = ComponentUtil.getData(listenOn,
                Shortcuts.ELEMENT_LOCATOR_JS_KEY);
        if (elementLocator == null) {
            return;
        }
        final String requiredModifiers = getModifierNames(modifiers, true);
        final String forbiddenModifiers = getModifierNames(modifiers, false);
        // #24974 ignore keydown events bubbling from a modal/popover nested
        // inside the delegate element (e.g. a nested dialog overlay). The
        // delegate re-dispatches a clone that loses the origin, so the guard
        // must run on the original event, before the clone.
        final boolean checkBoundary = !allowEventsFromNestedModals;
        final String[] keys = getKeyValues();

        // A declared locator is compared by its call, as setting the locator
        // again creates a new function for the same call
        final Object locatorKey = elementLocator instanceof JsFunction function
                && function.getDeclaredCall() != null
                        ? function.getDeclaredCall()
                        : elementLocator;
        final List<Object> delegateSettings = List.of(locatorKey,
                requiredModifiers, forbiddenModifiers, checkBoundary,
                resetFocusOnActiveElement, allowDefaultBehavior, List.of(keys));
        final Set<List<Object>> delegates = getOrInitListenData(listenOn);
        if (!delegates.add(delegateSettings)) {
            return;
        }
        // #10362 the delegate only prevents the default of keydown events that
        // match, to not block typing or existing shortcuts
        listenOn.getElement().executeJs(ShortcutJs.class)
                .registerKeydownDelegate(toLocatorFunction(elementLocator),
                        requiredModifiers, forbiddenModifiers, checkBoundary,
                        resetFocusOnActiveElement, allowDefaultBehavior, keys);
    }

    /**
     * Gets the function that locates the delegate element on the client, from a
     * locator given to {@link Shortcuts#setShortcutListenOnElement}.
     *
     * @param elementLocator
     *            the declared locator function, or the JavaScript expression of
     *            a locator given as a string
     * @return the locator function
     */
    private static JsFunction toLocatorFunction(Object elementLocator) {
        if (elementLocator instanceof JsFunction function) {
            return function;
        }
        // A locator given as a string is an expression that the browser
        // compiles, with the listen-on element as this
        return JsFunction.of("return (" + elementLocator + ");");
    }

    /**
     * Get the sent listenOn event data or initialize new listenOn data.
     *
     * @param listenOn
     *            component to listen events on
     * @return set with the settings of the delegates already registered
     */
    @SuppressWarnings("unchecked")
    private Set<List<Object>> getOrInitListenData(Component listenOn) {
        final Object componentData = ComponentUtil.getData(listenOn,
                LISTEN_ON_INITIALIZED);
        if (componentData != null) {
            return (Set<List<Object>>) componentData;
        }
        final HashSet<List<Object>> delegates = new HashSet<>();
        ComponentUtil.setData(listenOn, LISTEN_ON_INITIALIZED, delegates);
        listenOn.addDetachListener(event -> ComponentUtil
                .setData(event.getSource(), LISTEN_ON_INITIALIZED, null));
        return delegates;
    }

    /**
     * Wraps a {@link Key} instance. Makes it easier to compare the keys and
     * store them by hash.
     */
    private static class HashableKey implements Key {
        private final Key key;
        private Integer hashcode;

        HashableKey(Key key) {
            assert key != null;

            this.key = key;
        }

        @Override
        public int hashCode() {
            if (hashcode == null) {
                hashcode = Arrays.hashCode(key.getKeys().stream()
                        .map(String::toLowerCase).sorted(String::compareTo)
                        .toArray(String[]::new));
            }

            return hashcode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof HashableKey) {
                HashableKey other = (HashableKey) obj;

                // this is enough as key strings are unique for a key
                return key.matches(other.getKeys().get(0));
            }
            return false;
        }

        @Override
        public List<String> getKeys() {
            return key.getKeys();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if (listenOnComponents != null) {
            builder.append(Arrays.stream(listenOnComponents)
                    .map(component -> component.getClass().getSimpleName())
                    .collect(Collectors.joining(",")));
        }
        builder.append("]");
        return String.format(
                "%s [key = %s, modifiers = %s, owner = %s, listenOn = %s, "
                        + "default = %s, propagation = %s]",
                getClass().getSimpleName(),
                primaryKey != null ? primaryKey.getKeys().get(0) : "null",
                Arrays.toString(modifiers.stream().map(k -> k.getKeys().get(0))
                        .toArray()),
                lifecycleOwner != null
                        ? lifecycleOwner.getClass().getSimpleName()
                        : "null",
                builder.toString(), allowDefaultBehavior,
                allowEventPropagation);
    }

    private void initListenOnComponent() {
        listenOnComponents = registerOwnerListeners();
    }

    private void addListenOnDetachListeners() {
        if (!registrations.isEmpty()) {
            return;
        }
        for (Component component : listenOnComponents) {
            Registration registration = component.addDetachListener(
                    event -> removeAllListenerRegistrations());
            registrations.add(registration);
        }
    }

    /**
     * Bundles multiple {@link Registration Registrations} together. This is
     * used to group registrations that need to be created and removed together.
     */
    private static class CompoundRegistration implements Registration {
        private Set<Registration> registrations;

        CompoundRegistration(Registration... registrations) {
            this.registrations = new HashSet<>(Arrays.asList(registrations));
        }

        void addRegistration(Registration registration) {
            if (registration != null)
                registrations.add(registration);
        }

        @Override
        public void remove() {
            if (registrations != null) {
                registrations.forEach(Registration::remove);
                registrations = null;
            }
        }
    }
}
