package com.vaadin.flow.component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShortcutEvent extends ComponentEvent<Component> {
    private Component lifecycleOwner;
    private Key key;
    private Set<KeyModifier> keyModifiers;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     */
    public ShortcutEvent(Component source, boolean fromClient) {
        this(source, fromClient, null, null);
    }

    /**
     * @param source
     * @param fromClient
     * @param lifecycleOwner
     * @param key
     * @param keyModifiers
     */
    public ShortcutEvent(Component source, boolean fromClient,
                         Component lifecycleOwner, Key key,
                         KeyModifier... keyModifiers) {
        super(source, fromClient);
        this.lifecycleOwner = lifecycleOwner;
        this.key = key;
        this.keyModifiers = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(keyModifiers)));
    }

    /**
     * @return
     */
    public Component getLifecycleOwner() {
        return lifecycleOwner;
    }

    /**
     * @return
     */
    public Key getKey() {
        return key;
    }

    /**
     * @return
     */
    public Set<KeyModifier> getKeyModifiers() {
        return keyModifiers;
    }

    /**
     * Checks if the event matches the given {@link Key} and (optional)
     * {@link KeyModifier KeyModifiers}. If {@code key} is null or a wrong
     * number of {@code keyModifiers} is given, returns {@code false}.
     *
     * @param key           {@code key} to compare
     * @param keyModifiers  {@code keyModifiers} to compare
     * @return Did the given parameters match those in the event?
     */
    public boolean matches(Key key, KeyModifier... keyModifiers) {
        if (key == null) {
            return false;
        }
        if (keyModifiers.length != this.keyModifiers.size()) {
            return false;
        }
        List<String> keyStrings = Stream.of(keyModifiers)
                .map(k -> k.getKeys().get(0)).collect(Collectors.toList());
        return key.matches(this.key.getKeys().get(0)) && this.keyModifiers
                .stream().allMatch(k -> keyStrings.stream()
                        .anyMatch(k::matches));
    }
}
