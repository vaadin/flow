package com.vaadin.flow.component;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.shared.Registration;

/**
 * A registration object for both configuring and removing the registered
 * keyboard shortcut.
 *
 * @author Vaadin Ltd.
 * @since
 */
public class ShortcutRegistration implements Registration, Serializable {
    private boolean preventDefault = false;
    private boolean stopPropagation = false;

    private Set<Key> modifiers = new HashSet<>(2);
    private Key primaryKey = null;

    private ComponentRegistration ownerRegistration;
    private StateTree.ExecutionRegistration executionRegistration;
    private Set<ComponentRegistration> scopeRegistrations = new HashSet<>(2);

    private AtomicBoolean isDirty = new AtomicBoolean(false);

    private Runnable shortcutAction;

    ShortcutRegistration(Runnable shortcutAction, Component owner) {
        this.shortcutAction = shortcutAction;
        setOwner(owner);
    }

    /**
     * Fluent method for adding a char-based key as a primary key for
     * the shortcut.
     * @param character     Shortcut key, e.g. 'c' or 'C';
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration on(char character) {
        // addKey handles markDirty call
        addKey(charToKey(character));

        return this;
    }

    /**
     * Fluent method for adding a {@link Key}-based key as a primary key for the
     * shortcut. The parameter cannot be null.
     * @param key   Shortcut key
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration on(Key key) {
        if (key == null) {
            throw new IllegalArgumentException(
                    "Parameter key must not be null!");
        }

        // addKey handles markDirty call
        addKey(key);

        return this;
    }

    /**
     * Fluent method for adding a {@link KeyModifier} to the shortcut. E.g.
     * Alt or Ctrl.
     * @param modifier  Key modifier
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration on(KeyModifier modifier) {
        if (modifier == null) {
            throw new IllegalArgumentException(
                    "Parameter modifier must not be null!");
        }

        // addKey handles markDirty call
        addKey(modifier);

        return this;
    }

    /**
     * Fluent method for configuring alt modifier key.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration alt() {
        return on(KeyModifier.ALT);
    }

    /**
     * Fluent method for configuring ctrl modifier key.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration ctrl() {
        return on(KeyModifier.CONTROL);
    }

    /**
     * Fluent method for configuring meta modifier key.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration meta() {
        return on(KeyModifier.META);
    }

    /**
     * Fluent method for configuring shift modifier key.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration shift() {
        return on(KeyModifier.SHIFT);
    }

    /**
     * Prevent default event handling when the shortcut is invoked
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration preventDefault() {
        if (!preventDefault) {
            preventDefault = true;
            markDirty();
        }
        return this;
    }

    /**
     * Prevent the event from propagating upwards in the dom tree, when the
     * shortcut is invoked.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration stopPropagation() {
        if (!stopPropagation) {
            stopPropagation = true;
            markDirty();
        }
        return this;
    }

    /**
     * Designate a {@link Component} or multiple components as the invocation
     * scope of the shortcut. The shortcut will only be invoked when a HTML
     * element inside the given scope components has focus.
     * <p>
     *     If no scope is given for the shortcut, global scope is used instead.
     *     Global scope is the same as calling <code>shortcutRegistration.scope
     *     (UI.getCurrent())</code>.
     * @param scope         Primary scope of the shortcut
     * @param moreScopes    More scopes for the shortcut
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration scope(Component scope,
                                      Component... moreScopes) {
        if (scope == null) {
            throw new IllegalArgumentException(
                    "Parameter scope must not be null!");
        }

        markDirty();

        scopeRegistrations.forEach(ComponentRegistration::clear);

        scopeRegistrations = new HashSet<>(moreScopes.length + 1);

        Set<Component> componentSet = new HashSet<>(Arrays.asList(moreScopes));
        componentSet.add(scope);

        scopeRegistrations = componentSet.stream()
                .map(this::createScopeRegistration).collect(Collectors.toSet());

        return this;
    }

    /**
     * Binds the shortcut's life cycle to that of the given {@link Component}.
     * When the given <code>component is attached</code>, there shortcut is
     * attached in all attached scopes and when the given <code>component</code>
     * is detached, the shortcut is removed from all attached scopes.
     *
     * <p>
     *     If you want to use the life cycle <code>component</code> as the
     *     shortcut listening context, use {@link #scope(Component, Component...)}
     *     to achieve that.
     *<p>
     *     <b>Note:</b> the default life cycle <code>component</code> of a
     *     shortcut is the owning <code>component</code>. Calling this function
     *     will change the owner of the shortcut.
     *
     * @param component New owner of the shortcut
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration bindLifecycleTo(Component component) {
        // this does not require setDirty as the shortcut configuration is
        // not affected by this change

        if (component == null) {
            throw new InvalidParameterException(
                    "Parameter component must not be null!");
        }

        setOwner(component);
        return this;
    }

    /**
     * Removes all attached listeners related to the shortcut. Clears
     * {@link Registration registrations} tied to the owner of the shortcut and
     * registrations tied to the scope {@link Component components}.
     */
    @Override
    public void remove() {
        if (executionRegistration != null) {
            executionRegistration.remove();
        }
        if (ownerRegistration != null) {
            ownerRegistration.clear();
        }
        if (scopeRegistrations != null) {
            for (ComponentRegistration registration : scopeRegistrations) {
                registration.clear();
            }
        }

        shortcutAction = null;
    }

    private void setOwner(Component owner) {
        assert owner != null;

        if (ownerRegistration != null) {
            ownerRegistration.clear();
        }
        ownerRegistration = createOwnerRegistration(owner);
    }

    private void addKey(Key key) {
        assert key != null;

        HashableKey hashableKey = new HashableKey(key);

        if (Key.isModifier(key)) {
            if (!modifiers.contains(hashableKey)) {
                modifiers.add(hashableKey);
                markDirty();
            }
        }
        else {
            if (primaryKey == null || !primaryKey.equals(hashableKey)) {
                primaryKey = hashableKey;
                markDirty();
            }
        }
    }

    private void markDirty() {
        synchronized (this) {
            if (isDirty.get()) return;
            isDirty.set(true);
        }

        // either owner is attached or it is UI
        if (ownerRegistration.component.getUI().isPresent()) {
            executionRegistration = UI.getCurrent().beforeClientResponse(
                    ownerRegistration.component, beforeClientResponseConsumer);
        }
        /*
        else {
            see createOwnerRegistration() for what happens when the owner is
            not attached or it is not an instance of UI
        }
        */
    }

    private void markClean() {
        synchronized (this) {
            if (!isDirty.get()) return;
            isDirty.set(false);
            executionRegistration = null;
        }
    }

    private String filterText() {
        String modifierFilter = modifiers.stream().filter(Key::isModifier)
                .map(modifier ->
                        "event.getModifierState('" +
                                modifier.getKeys().get(0) + "')")
                .collect(Collectors.joining(" && "));

        return generateEventKeyJSMatcher(primaryKey) + " && " +
                (modifierFilter.isEmpty() ? "true" : modifierFilter);
    }

    private static Key charToKey(char c) {
        return Key.of(("" + c).toLowerCase());
    }

    private static String generateEventKeyJSMatcher(Key key) {
        // will now allow shortcut to happen without primary key
        if (key == null) return "false";
        else return "[" + key.getKeys().stream()
                .map(s -> "'" + s.toLowerCase() + "'")
                .collect(Collectors.joining(",")) +
                "].indexOf(event.key.toLowerCase()) !== -1";
    }

    private void updateDomRegistrationFor(ComponentRegistration registration) {
        assert registration != null;

        registration.updateDomRegistration(shortcutAction, filterText(),
                preventDefault, stopPropagation);
    }

    private ComponentRegistration createOwnerRegistration(Component owner) {
        assert owner != null;

        ComponentRegistration registration = new ComponentRegistration(owner);

        owner.addAttachListener(e -> executionRegistration = UI.getCurrent()
                .beforeClientResponse(
                        ownerRegistration.component,
                        beforeClientResponseConsumer));

        owner.addDetachListener(e -> scopeRegistrations
                .forEach(ComponentRegistration::removeDomRegistration));

        return registration;
    }

    private ComponentRegistration createScopeRegistration(Component scope) {
        assert scope != null;

        ComponentRegistration registration = new ComponentRegistration(scope);

        if (!(scope instanceof UI)) {
            scope.addAttachListener(
                    attachEvent -> updateDomRegistrationFor(registration));

            scope.addDetachListener(
                    detachEvent -> registration.removeDomRegistration());
        }

        // either the scope is an active UI, or the component is attached to an
        // active UI - in either case, we want to update dom registration
        if (scope.getUI().isPresent()) {
            updateDomRegistrationFor(registration);
        }

        return registration;
    }

    private final SerializableConsumer<ExecutionContext>
            beforeClientResponseConsumer = executionContext -> {
        if (scopeRegistrations == null || scopeRegistrations.size() == 0) {
            UI ui = UI.getCurrent();
            if (ui != null) {
                scopeRegistrations = new HashSet<>();
                scopeRegistrations.add(createScopeRegistration(ui));
            }
            else {
                throw new IllegalStateException(
                        String.format("Unable to complete a %s without a " +
                                        "scope when %s is not available. Use " +
                                        "scope(Component, Component...) to " +
                                        "define scope.",
                        ShortcutRegistration.class.getSimpleName(),
                        UI.class.getSimpleName()));
            }
        }

        if (primaryKey == null) {
            throw new IllegalStateException(String.format("Unable to " +
                            "complete %s if the shortcut does not have a primary " +
                            "key. Use .on(Character) or .on(Key) to define a " +
                            "primary key, such as 'L' or Key.F6.",
                    ShortcutRegistration.class.getSimpleName()));
        }

        for (ComponentRegistration scopeRegistration : scopeRegistrations) {
            updateDomRegistrationFor(scopeRegistration);
        }

        markClean();
    };

    /**
     * Class for storing all the registration information tied to a component
     * and shortcut. These include attach and detach listener registrations,
     * domListenerRegistration, and keydown listener registration.
     *
     * These are used to keep track of the components state and the resulting
     * state of the shortcut listening mechanisms
     */
    private static class ComponentRegistration {
        private Component component;
        private Registration attachRegistration;
        private Registration detachRegistration;
        private Registration keydownRegistration;
        private DomListenerRegistration domRegistration;

        ComponentRegistration(Component component) {
            assert component != null;

            this.component = component;
        }

        void removeDomRegistration() {
            if (keydownRegistration != null) keydownRegistration.remove();
            keydownRegistration = null;
            domRegistration = null;
        }


        void updateDomRegistration(Runnable shortcutAction, String filter,
                                   boolean preventDefault,
                                   boolean stopPropagation) {
            assert shortcutAction != null;
            assert filter != null;

            // we will only come here as a result of a `beforeClientResponse`
            // callback and so we will most likely have the registration
            // available. ComponentRegistration has this available if it is
            // a scope registration and the component is attached to the UI

            if (keydownRegistration == null) {
                if (component.getUI().isPresent()) {
                    keydownRegistration = ComponentUtil.addListener(
                            component,
                            KeyDownEvent.class,
                            e -> shortcutAction.run(),
                            domRegistration -> {
                                this.domRegistration = domRegistration;
                                domRegistration.setFilter(filter);
                                if (preventDefault) {
                                    domRegistration.addEventData(
                                            "event.preventDefault()");
                                }
                                if (stopPropagation) {
                                    domRegistration.addEventData(
                                            "event.stopPropagation()");
                                }
                            });
                }
            }
            else {
                domRegistration.setFilter(filter);
                if (preventDefault) {
                    domRegistration.addEventData("event.preventDefault()");
                }
                if (stopPropagation) {
                    domRegistration.addEventData("event.stopPropagation()");
                }
            }
        }

        void clear() {
            if (attachRegistration != null)
                attachRegistration.remove();
            if (detachRegistration != null)
                detachRegistration.remove();
            if (keydownRegistration != null)
                keydownRegistration.remove();
            domRegistration = null;
        }

        @Override
        public int hashCode() {
            return component.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComponentRegistration)) return false;

            return ((ComponentRegistration)obj).component.equals(component);
        }
    }

    /**
     * Class used to wrap a {@link Key} instance. Makes it easier to compare the
     * keys and store them by hash.
     */
    private static class HashableKey implements Key {
        private Key key;
        private Integer hashcode;

        HashableKey(Key key) {
            assert key != null;

            this.key = key;
        }

        @Override
        public int hashCode() {
            if (hashcode == null) {
                hashcode = Arrays.hashCode(key.getKeys().stream()
                        .map(String::toLowerCase)
                        .sorted(String::compareTo)
                        .toArray(String[]::new));
            }

            return hashcode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof HashableKey) {
                HashableKey other = (HashableKey)obj;
                if (other.key.getKeys().size() != key.getKeys().size())
                    return false;

                for (String k : key.getKeys()) {
                    if (!other.key.matches(k)) return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public List<String> getKeys() {
            return key.getKeys();
        }
    }

}
