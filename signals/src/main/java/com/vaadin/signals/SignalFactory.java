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
package com.vaadin.signals;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a signal instance based on a name. This encapsulates different
 * strategies for creating instances that are e.g. synchronized across a
 * cluster, shared within the same JVM, or isolated from any other instance.
 */
@FunctionalInterface
public interface SignalFactory {
    /**
     * A signal factory that always returns a new instance that is not shared.
     * This factory does not support the optional removal methods.
     */
    SignalFactory IN_MEMORY_EXCLUSIVE = ignore -> new NodeSignal();

    /**
     * A signal factory that always returns the same signal for the same name
     * within the same JVM. This factory supports the optional removal methods.
     */
    SignalFactory IN_MEMORY_SHARED = new SignalFactory() {
        private final Map<String, NodeSignal> instances = new ConcurrentHashMap<>();

        @Override
        public NodeSignal node(String name) {
            return instances.computeIfAbsent(name, ignore -> new NodeSignal());
        }

        @Override
        public void remove(String name) {
            instances.remove(name);
        }

        @Override
        public void clear() {
            instances.clear();
        }
    };

    /**
     * Gets a node signal for the given name.
     *
     * @param name
     *            the name to use, not <code>null</code>
     * @return a node signal, not <code>null</code>
     */
    NodeSignal node(String name);

    /**
     * Gets a value signal of the given type for the given name.
     *
     * @param <T>
     *            the value type
     * @param name
     *            the name to use, not <code>null</code>
     * @param valueType
     *            the value type to use, not <code>null</code>
     * @return a value signal, not <code>null</code>
     */
    default <T> ValueSignal<T> value(String name, Class<T> valueType) {
        return node(name).asValue(valueType);
    }

    /**
     * Gets a value signal of the given type for the given name and initializes
     * it with the provided default value if the signal doesn't already have a
     * value. If the implementation returns an existing signal instance that
     * already had a non-null value, then the provided default value is ignored.
     *
     * @param <T>
     *            the value type
     * @param name
     *            the name to use, not <code>null</code>
     * @param valueType
     *            the value type to use, not <code>null</code>
     * @param defaultValue
     *            the default value to use, not <code>null</code>
     * @return a value signal, not <code>null</code>
     */
    default <T> ValueSignal<T> value(String name, Class<T> valueType,
            T defaultValue) {
        Objects.requireNonNull(defaultValue,
                "The default value cannot be null");

        ValueSignal<T> signal = value(name, valueType);
        if (signal.peek() == null) {
            signal.replace(null, defaultValue);
        }
        return signal;
    }

    /**
     * Gets a value signal for the given name and initializes it with the
     * provided default value if the signal doesn't already have a value. If the
     * implementation returns an existing signal instance that already had a
     * non-null value, then the provided default value is ignored. The signal
     * value type is based on the type ({@link Object#getClass()}) of the
     * default value instance.
     *
     * @param <T>
     *            the value type
     * @param name
     *            the name to use, not <code>null</code>
     * @param defaultValue
     *            the default value to use, not <code>null</code>
     * @return a value signal, not <code>null</code>
     */
    default <T> ValueSignal<T> value(String name, T defaultValue) {
        Objects.requireNonNull(defaultValue,
                "The default value cannot be null");

        @SuppressWarnings("unchecked")
        Class<T> valueType = (Class<T>) defaultValue.getClass();
        return value(name, valueType, defaultValue);
    }

    /**
     * Gets a number signal for the given name.
     *
     * @param name
     *            the name to use, not <code>null</code>
     * @return a number signal, not <code>null</code>
     */
    default NumberSignal number(String name) {
        return node(name).asNumber();
    }

    /**
     * Gets a number signal for the given name and initializes it with the
     * provided default value if the signal doesn't already have a value.
     *
     * @param name
     *            the name to use, not <code>null</code>
     * @param defaultValue
     *            the default value to use
     * @return a number signal, not <code>null</code>
     */
    default NumberSignal number(String name, double defaultValue) {
        return value(name, Double.class, defaultValue).asNode().asNumber();
    }

    /**
     * Gets a list signal with the given element type for the given name.
     *
     * @param <T>
     *            the element type
     * @param name
     *            the to use, not <code>null</code>
     * @param elementType
     *            the element type, not <code>null</code>
     * @return a list signal, not <code>null</code>
     */
    default <T> ListSignal<T> list(String name, Class<T> elementType) {
        return node(name).asList(elementType);
    }

    /**
     * Gets a map signal with the given element type for the given name.
     *
     * @param <T>
     *            the element type
     * @param name
     *            the to use, not <code>null</code>
     * @param elementType
     *            the element type, not <code>null</code>
     * @return a map signal, not <code>null</code>
     */
    default <T> MapSignal<T> map(String name, Class<T> elementType) {
        return node(name).asMap(elementType);
    }

    /**
     * Removes any previous association for the given name. Removing an
     * association typically means that memory previously associated with the
     * name is freed and a subsequent invocation with the same name will create
     * a new signal.
     * <p>
     * This is an optional operation. Implementations that don't support removal
     * throw {@link UnsupportedOperationException}.
     *
     * @param name
     *            the name to remove, not {@link NullPointerException}
     * @throws UnsupportedOperationException
     *             if this implementation doesn't support removal
     */
    default void remove(String name) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes all name associations. Removing an association typically means
     * that memory previously associated with the name is freed and a subsequent
     * invocation with the same name will create a new signal.
     * <p>
     * This is an optional operation. Implementations that don't support
     * clearing may throw {@link UnsupportedOperationException}.
     *
     * @throws UnsupportedOperationException
     *             if this implementation doesn't support clearing
     */
    default void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
