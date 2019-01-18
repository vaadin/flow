package com.vaadin.flow.component;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collection;
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

    private boolean shortcutActive = false;

    private SerializableSupplier<Component> handlerOwnerSupplier;

    // used to determine, if we need to do something before client response
    private AtomicBoolean isDirty = new AtomicBoolean(false);

    private Command shortcutCommand;

    /**
     * @param lifecycleOwner
     *              This is the component which controls when the shortcut is
     *              actually active. If the component is either detached or
     *              invisible, the shortcut will not be active
     * @param handlerOwnerSupplier
     *              Supplier for component to which the shortcut listener is
     *              bound to. Supplier is given in order to get around some
     *              cases where the component might not be immediately
     *              available.
     * @param command
     *              The code to execute when the shortcut is invoked
     * @param key
     *              Primary key of the shortcut. This can not be a modifier key.
     */
    ShortcutRegistration(Component lifecycleOwner,
                         SerializableSupplier<Component> handlerOwnerSupplier,
                         Command command, Key key) {
        if (Key.isModifier(key)) {
            throw new InvalidParameterException(String.format("Parameter " +
                    "'key' cannot belong to %s",
                    KeyModifier.class.getSimpleName()));
        }

        shortcutCommand = command;
        this.handlerOwnerSupplier = handlerOwnerSupplier;
        setLifecycleOwner(lifecycleOwner);

        // addKey cannot be called without lifecycleOwner
        addKey(key);
    }

    /**
     * Configures {@link KeyModifier KeyModifiers} for the shortcut.
     * Calling this method will overwrite any previously set modifier keys.
     * Hence, calling {@code shortcutRegistration.withModifiers();} will remove
     * all previously set modifier keys.
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
     * Fluently adds {@link KeyModifier#ALT} to the shortcut's modifiers.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withAlt() {
        addKey(KeyModifier.ALT);
        return this;
    }

    /**
     * Fluently adds {@link KeyModifier#CONTROL} to the shortcut's modifiers.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withCtrl() {
        addKey(KeyModifier.CONTROL);
        return this;
    }

    /**
     * Fluently adds {@link KeyModifier#META} to the shortcut's modifiers.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withMeta() {
        addKey(KeyModifier.META);
        return this;
    }

    /**
     * Fluently adds {@link KeyModifier#SHIFT} to the shortcut's modifiers.
     * @return this <code>ShortcutRegistration</code>
     */
    public ShortcutRegistration withShift() {
        addKey(KeyModifier.SHIFT);
        return this;
    }

    /**
     * Allows the default keyboard event handling when the shortcut is invoked
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
     * Allow the event to propagate upwards in the dom tree, when the
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
     * Binds the shortcut's life cycle to that of the given {@link Component}.
     * When the given {@code component} is attached, the shortcut's listener is
     * attached to the {@code Component} that owns the shortcut. When the given
     * {@code component} is detached, so is the listener.
     * is detached, the shortcut is removed from all attached scopes.
     *
     * @param component New lifecycle owner of the shortcut
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

    /**
     * Is the shortcut active on the current UI. For this to be true, the
     * lifecycle owner needs to be attached and visible and handler owner
     * needs to be attached.
     *
     * @return  Is the shortcut active
     */
    public boolean isShortcutActive() {
        return shortcutActive;
    }

    /**
     * Get the primary {@link Key} of the shortcut. Primary key can be any key besides
     * modifier keys.
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
        return new HashSet<>(modifiers);
    }

    /**
     * Is the shortcut preventing default key behaviour.
     *
     * @return Prevents default behavior
     */
    public boolean preventsDefault() {
        return preventDefault;
    }

    /**
     * Is the shortcut stopping the keyboard event from propagating up the DOM
     * tree.
     *
     * @return Stops propagation
     */
    public boolean stopsPropagation() {
        return stopPropagation;
    }

    /**
     * {@link Component} which owns the shortcuts key event listener.
     * @return Component
     */
    public Component getOwner() {
        return handlerOwner;
    }

    /**
     * {@link Component} which controls when the shortcut is active and when it
     * is not.
     * @return Component
     * @see #isShortcutActive()
     */
    public Component getLifecycleOwner() {
        return lifecycleOwner;
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

        // if lifecycleOwner is attached, we'll register new
        // beforeClientResponse callback. Otherwise we'll need to wait for the
        // lifecycleOwner's attach-callback to do it
        queueBeforeExecutionCallback();
    }

    private void markClean() {
        synchronized (this) {
            if (!isDirty.get()) return;
            isDirty.set(false);
            executionRegistration = null;
        }
    }

    private String filterText() {
        return generateEventKeyFilter(primaryKey) + " && " +
                generateEventModifierFilter(modifiers);
    }

    private void updateHandlerListenerRegistration() {
        assert handlerOwner != null;

        if (handlerListenerRegistration == null) {
            if (handlerOwner.getUI().isPresent()) {
                handlerListenerRegistration = new CompoundRegistration();

                Registration keydownRegistration = ComponentUtil.addListener(
                        handlerOwner,
                        KeyDownEvent.class,
                        e -> {
                            if (lifecycleOwner.isVisible()) {
                                shortcutCommand.execute();
                            }
                        },
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
                            r instanceof DomListenerRegistration)
                    .findFirst();

            registration.ifPresent(r -> {
                DomListenerRegistration listenerRegistration =
                        (DomListenerRegistration) r;

                String filterText = filterText();
                /*
                    Due to https://github.com/vaadin/flow/issues/4871 we are not
                    able to use setEventData for these values, so we hack the
                    filter.
                 */
                if (preventDefault) {
                    filterText += "&& (event.preventDefault() || true)";
                }
                if (stopPropagation) {
                    filterText += "&& (event.stopPropagation() || true)";
                }
                listenerRegistration.setFilter(filterText);

                shortcutActive = true;
            });
        }
    }

    private void registerLifecycleOwner(Component owner) {
        assert owner != null;

        lifecycleOwner = owner;

        // since we are attached, UI should be available
        Registration attachRegistration = owner.addAttachListener(e ->
                queueBeforeExecutionCallback());

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
        shortcutActive = false;
    }

    private boolean isLifecycleOwnerAttached() {
        return lifecycleOwner != null && lifecycleOwner.getUI().isPresent();
    }

    private void queueBeforeExecutionCallback() {
        if (!isLifecycleOwnerAttached()) {
            return;
        }

        if (executionRegistration != null) {
            executionRegistration.remove();
        }
        // isLifecycleOwnerAttached checks for UI status
        executionRegistration = lifecycleOwner.getUI().get()
                .beforeClientResponse(lifecycleOwner,
                        beforeClientResponseConsumer);
    }

    private static String generateEventModifierFilter(
            Collection<Key> modifiers) {

        if (modifiers.size() == 0) return "true";

        return modifiers.stream().filter(Key::isModifier)
                .map(modifier ->
                        "event.getModifierState('" +
                                modifier.getKeys().get(0) + "')")
                .collect(Collectors.joining(" && "));
    }

    private static String generateEventKeyFilter(Key key) {
        // will now allow shortcut to happen without primary key
        if (key == null) return "false";
        String keyList = "[" + key.getKeys().stream().map(s -> "'" + s + "'")
                .collect(Collectors.joining(",")) + "]";
        return  "(" + keyList + ".indexOf(event.code) !== -1 || " +
                keyList + ".indexOf(event.key) !== -1)";
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
                (primaryKey != null ? primaryKey.getKeys().get(0) : "null"),
                Arrays.toString(modifiers.stream()
                        .map(k -> k.getKeys().get(0)).toArray()),
                (lifecycleOwner != null) ? lifecycleOwner.getClass()
                        .getSimpleName() : "null");
    }

    /**
     * Class for bundling multiple {@link Registration Registrations} together.
     * This is used to group registrations that need to be created and removed
     * together.
     */
    private static class CompoundRegistration implements Registration {
        Set<Registration> registrations;

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