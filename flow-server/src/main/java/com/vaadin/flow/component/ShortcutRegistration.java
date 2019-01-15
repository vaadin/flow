package com.vaadin.flow.component;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

/**
 * A registration object for both configuring and removing the registered
 * keyboard shortcut.
 *
 * @author Vaadin Ltd.
 * @since
 */
public class ShortcutRegistration implements Registration, Serializable {
    private boolean preventDefault = true;
    private boolean stopPropagation = true;

    private Set<Key> modifiers = new HashSet<>(2);
    private Key primaryKey = null;

    private StateTree.ExecutionRegistration executionRegistration;
    // lifecycle owner
    // usually lifecycleRegistration == listenerRegistration
    private CompoundRegistration lifecycleRegistration;
    private Component lifecycleOwner;
    // event listener owner
    private CompoundRegistration handlerStateRegistration;
    private CompoundRegistration handlerListenerRegistration;
    private Component handlerOwner;


    private SerializableSupplier<Component> handlerOwnerSupplier;

    // used to determine, if we need to do something before client response
    private AtomicBoolean isDirty = new AtomicBoolean(false);

    private Command shortcutCommand;

    private ShortcutRegistration(SerializableSupplier<Component> handlerOwnerSupplier,
                                 Command command, Key key) {
        assert handlerOwnerSupplier != null;
        assert command != null;
        assert key != null;

        shortcutCommand = command;
        this.handlerOwnerSupplier = handlerOwnerSupplier;
        addKey(key);
    }

    ShortcutRegistration(Component lifecycleOwner,
                         SerializableSupplier<Component> handlerOwnerSupplier,
                         Command command, Key key) {
        this(handlerOwnerSupplier, command, key);
        setLifecycleOwner(lifecycleOwner);
    }

    ShortcutRegistration(Component lifecycleOwner,
                         SerializableSupplier<Component> handlerOwnerSupplier,
                         Command command, char character) {
        this(handlerOwnerSupplier, command, charToKey(character));
        setLifecycleOwner(lifecycleOwner);
    }

    /**
     * Configures {@link KeyModifier KeyModifiers} for the shortcut.
     * Calling this method will overwrite any previously set modifier keys.
     * Hence, calling
     * <code>
     *     shortcutRegistration.withModifiers();
     * </code>
     * will remove all previously set modifier keys.
     *
     * @param keyModifiers  Key modifiers. Can be empty.
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
     * Fluent method for configuring alt modifier key.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withAlt() {
        addKey(KeyModifier.ALT);
        return this;
    }

    /**
     * Fluent method for configuring ctrl modifier key.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withCtrl() {
        addKey(KeyModifier.CONTROL);
        return this;
    }

    /**
     * Fluent method for configuring meta modifier key.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withMeta() {
        addKey(KeyModifier.META);
        return this;
    }

    /**
     * Fluent method for configuring shift modifier key.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withShift() {
        addKey(KeyModifier.SHIFT);
        return this;
    }

    /**
     * Prevent default event handling when the shortcut is invoked
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration allowBrowserDefault() {
        if (preventDefault) {
            preventDefault = false;
            prepareForClientResponse();
        }
        return this;
    }

    /**
     * Prevent the event from propagating upwards in the dom tree, when the
     * shortcut is invoked.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration allowEventPropagation() {
        if (stopPropagation) {
            stopPropagation = false;
            prepareForClientResponse();
        }
        return this;
    }


    /**
     * todo
     * Binds the shortcut's life cycle to that of the given {@link Component}.
     * When the given <code>component is attached</code>, there shortcut is
     * attached in all attached scopes and when the given <code>component</code>
     * is detached, the shortcut is removed from all attached scopes.
     *
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
                    String.format(Shortcuts.NULL, "component"));
        }

        setLifecycleOwner(component);
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
            executionRegistration = null;
        }
        if (lifecycleRegistration != null) {
            lifecycleRegistration.remove();
            lifecycleRegistration = null;
        }
        if (handlerStateRegistration != null) {
            handlerStateRegistration.remove();
            handlerStateRegistration = null;
        }
        if (handlerListenerRegistration != null) {
            handlerListenerRegistration.remove();
            handlerListenerRegistration = null;
        }

        lifecycleOwner = null;
        handlerOwner = null;

        shortcutCommand = null;
    }

    private void setLifecycleOwner(Component owner) {
        assert owner != null;

        if (lifecycleRegistration != null) {
            lifecycleRegistration.remove();
        }

        registerLifecycleOwner(owner);
    }

    private void addKey(Key key) {
        assert key != null;

        HashableKey hashableKey = new HashableKey(key);

        if (Key.isModifier(key)) {
            if (!modifiers.contains(hashableKey)) {
                modifiers.add(hashableKey);
                prepareForClientResponse();
            }
        }
        else {
            if (primaryKey == null || !primaryKey.equals(hashableKey)) {
                primaryKey = hashableKey;
                prepareForClientResponse();
            }
        }
    }

    private void prepareForClientResponse() {
        assert lifecycleOwner != null;

        synchronized (this) {
            if (isDirty.get()) return;
            isDirty.set(true);
        }

        // either lifecycle owner is attached or it is UI
        if (lifecycleOwner.getUI().isPresent()) {
            executionRegistration = UI.getCurrent().beforeClientResponse(
                    lifecycleOwner, beforeClientResponseConsumer);
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

    private void updateHandlerListenerRegistration() {
        assert handlerOwner != null;

        if (handlerListenerRegistration == null) {
            if (handlerOwner.getUI().isPresent()) {
                handlerListenerRegistration = new CompoundRegistration();

                Registration keydownRegistration = ComponentUtil.addListener(
                        handlerOwner,
                        KeyDownEvent.class,
                        e -> shortcutCommand.execute(),
                        domRegistration -> {
                            handlerListenerRegistration.addRegistration(
                                    domRegistration);
                            configureHandlerListenerRegistration();
                        });
                handlerListenerRegistration.addRegistration(
                        keydownRegistration);
            }
        }
        else {
            configureHandlerListenerRegistration();
        }
    }

    private void configureHandlerListenerRegistration() {
        if (handlerListenerRegistration != null) {
            Optional<Registration> registration = handlerListenerRegistration
                    .registrations.stream().filter(r ->
                            r.getClass().equals(DomListenerRegistration.class))
                    .findFirst();

            registration.ifPresent(r -> {
                DomListenerRegistration listenerRegistration =
                        (DomListenerRegistration) r;
                listenerRegistration.setFilter(filterText());
                if (preventDefault) {
                    listenerRegistration.addEventData("event.preventDefault()");
                }
                if (stopPropagation) {
                    listenerRegistration.addEventData(
                            "event.stopPropagation()");
                }
            });
        }
    }

    private void registerLifecycleOwner(Component owner) {
        assert owner != null;

        lifecycleOwner = owner;

        // since we are attached, UI should be available
        Registration attachRegistration = owner.addAttachListener(e -> {
            if (executionRegistration != null) {
                executionRegistration.remove();
            }
            executionRegistration = UI.getCurrent()
                    .beforeClientResponse(
                            lifecycleOwner,
                            beforeClientResponseConsumer);
        });

        // remove shortcut listener when detached
        Registration detachRegistration = owner.addDetachListener(e ->
                removeHandlerListener());

        lifecycleRegistration = new CompoundRegistration(attachRegistration,
                detachRegistration);
    }

    private void registerOwnerListener() {
        assert handlerOwnerSupplier != null;

        handlerOwner = handlerOwnerSupplier.get();

        if (handlerOwner == null) {
            throw new IllegalStateException(
                    String.format("Could register shortcut listener for %s. " +
                                    "%s<%s> supplied a null value.",
                    this.toString(),
                    SerializableSupplier.class.getSimpleName(),
                    Component.class.getSimpleName()));
        }

        if (!(handlerOwner instanceof UI)) {
            handlerStateRegistration = new CompoundRegistration();
            handlerStateRegistration.addRegistration(
                    handlerOwner.addAttachListener(
                            attachEvent -> updateHandlerListenerRegistration()));
            handlerStateRegistration.addRegistration(
                    handlerOwner.addDetachListener(
                            detachEvent -> removeHandlerListener()));
        }

        // either the scope is an active UI, or the component is attached to an
        // active UI - in either case, we want to update dom registration
        if (handlerOwner.getUI().isPresent()) {
            updateHandlerListenerRegistration();
        }
    }

    private void removeHandlerListener() {
        if (handlerListenerRegistration != null) {
            handlerListenerRegistration.remove();
            handlerListenerRegistration = null;
        }
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

    private final SerializableConsumer<ExecutionContext>
            beforeClientResponseConsumer = executionContext -> {
        if (handlerOwner == null) {
            registerOwnerListener();
        }

        updateHandlerListenerRegistration();

        markClean();
    };

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
        return  String.format(
                "%s [key = %s, modifiers = %s, lifecycle owner = %s]",
                getClass().getSimpleName(),
                primaryKey.getKeys().get(0),
                Arrays.toString(modifiers.stream()
                        .map(k -> k.getKeys().get(0)).toArray()),
                (lifecycleOwner == null) ? "null" : lifecycleOwner.getClass()
                        .getSimpleName());
    }

    /**
     * TODO
     * Class for storing all the registration information tied to a component
     * and shortcut. These include attach and detach listener registrations,
     * domListenerRegistration, and keydown listener registration.
     *
     * These are used to keep track of the components state and the resulting
     * state of the shortcut listening mechanisms
     */
    private static class CompoundRegistration implements Registration {
        Set<Registration> registrations;

        CompoundRegistration(Registration... registrations) {
            this.registrations = new HashSet<>(Arrays.asList(registrations));
        }

        void addRegistration(Registration registration) {
            registrations.add(registration);
        }

        @Override
        public void remove() {
            registrations.forEach(Registration::remove);
            registrations = null;
        }
    }
}