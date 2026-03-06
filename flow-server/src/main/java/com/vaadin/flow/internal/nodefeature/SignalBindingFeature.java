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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableBiPredicate;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.signals.Signal;

/**
 * Node feature for binding {@link Signal}s to various properties of a node.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@NullMarked
public class SignalBindingFeature extends ServerSideFeature {

    public static final String TEXT = "text";
    public static final String CLASSES = "classes/";
    public static final String CLASS_GROUP = "classes/*";
    public static final String ENABLED = "enabled";
    public static final String VALUE = "value";
    public static final String THEMES = "themes/";
    public static final String THEME_GROUP = "themes/*";
    public static final String HTML_CONTENT = "htmlContent";
    public static final String CHILDREN = "children";
    public static final String ITEMS = "items";

    private final Map<String, SignalBinding> values = new HashMap<>();

    private record SignalBinding(Signal<?> signal,
            @Nullable SerializableConsumer<?> writeCallback)
            implements
                Serializable {
    }

    /**
     * Creates a SignalBindingFeature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public SignalBindingFeature(StateNode node) {
        super(node);
    }

    /**
     * Sets a binding for the given key.
     *
     * @param key
     *            the key
     * @param signal
     *            the signal
     */
    public void setBinding(String key, Signal<?> signal) {
        setBinding(key, signal, null);
    }

    /**
     * Sets a binding for the given key with a write callback.
     *
     * @param key
     *            the key
     * @param signal
     *            the signal
     * @param writeCallback
     *            the callback to propagate value changes back, or
     *            <code>null</code> for a read-only binding
     */
    public void setBinding(String key, Signal<?> signal,
            @Nullable SerializableConsumer<?> writeCallback) {
        values.put(key, new SignalBinding(signal, writeCallback));
    }

    /**
     * Checks whether there is a binding for the given key.
     *
     * @param key
     *            the key
     * @return true if there is a binding for the given key, false otherwise
     */
    public boolean hasBinding(String key) {
        return values.containsKey(key);
    }

    /**
     * Checks whether there is any binding with a key starting with the given
     * prefix.
     *
     * @param keyPrefix
     *            the key prefix
     * @return true if there is any binding with a key starting with the given
     *         prefix, false otherwise
     */
    public boolean hasAnyBinding(String keyPrefix) {
        return values.keySet().stream()
                .anyMatch(key -> key.startsWith(keyPrefix));
    }

    /**
     * Gets the write callback for the given key.
     *
     * @param key
     *            the key
     * @param <T>
     *            the type of the consumer value
     * @return the write callback for the given key, or null if no callback is
     *         set
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable SerializableConsumer<T> getWriteCallback(String key) {
        SignalBinding binding = values.get(key);
        return binding != null ? (SerializableConsumer<T>) binding.writeCallback
                : null;
    }

    /**
     * Gets the signal bound to the given key.
     *
     * @param key
     *            the key
     * @param <T>
     *            the type of the signal value
     * @return the signal bound to the given key, or null if no signal is bound
     */
    @SuppressWarnings("unchecked")
    public <T extends @Nullable Object> @Nullable Signal<T> getSignal(
            String key) {
        SignalBinding binding = values.get(key);
        return binding != null ? (Signal<T>) binding.signal : null;
    }

    /**
     * Updates the signal value by invoking the write callback for the given
     * key. The callback is expected to update the signal value, and this method
     * will check whether the signal value was updated to the expected new
     * value. If the signal value differs from the expected new value after the
     * callback, the revert callback will be invoked with the current signal
     * value to revert the change.
     *
     * @param key
     *            the key for which to update the signal value
     * @param oldValue
     *            the old value before the update, used for comparison in case
     *            of revert
     * @param newValue
     *            the expected new value to be set by the write callback
     * @param valueEquals
     *            a predicate to compare signal values for equality
     * @param revertCallback
     *            a callback to revert the component value to the updated
     *            signal's value if the signal value does not match the expected
     *            new value after invoking the write callback
     * @return true if the signal value was updated to the expected new value,
     *         false if a revert was performed
     * @param <T>
     *            the type of the signal value
     */
    @SuppressWarnings("unchecked")
    public <T extends @Nullable Object> boolean updateSignalByWriteCallback(
            String key, T oldValue, T newValue,
            SerializableBiPredicate<T, T> valueEquals,
            SerializableConsumer<T> revertCallback) {
        SignalBinding binding = values.get(key);
        if (binding == null || binding.writeCallback == null) {
            // Read-only binding: revert and throw
            revertCallback.accept(oldValue);
            throw new IllegalStateException(
                    "Cannot set value on a read-only signal binding. "
                            + "Provide a write callback to enable two-way binding.");
        }
        ((SerializableConsumer<T>) binding.writeCallback).accept(newValue);
        // Re-consult the signal after the callback
        T signalValue = ((Signal<T>) binding.signal).peek();
        if (!valueEquals.test(signalValue, newValue)) {
            // Signal value differs, revert
            revertCallback.accept(signalValue);
            // no need to fire event, signal change triggered that
            return false;
        }
        return true;
    }

}
