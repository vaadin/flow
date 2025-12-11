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
package com.vaadin.signals.function;

/**
 * Computes a new signal value based on the current value, enabling atomic
 * compare-and-swap updates with automatic retry on conflicts.
 * <p>
 * The updater function may be called multiple times if concurrent modifications
 * occur. It should be side-effect free to ensure correctness during retries.
 *
 * @param <T>
 *            the signal value type
 */
@FunctionalInterface
public interface SignalUpdater<T> {
    /**
     * Computes a new value based on the current value.
     *
     * @param currentValue
     *            the current signal value, may be <code>null</code>
     * @return the new value to set, may be <code>null</code>
     */
    T update(T currentValue);
}
