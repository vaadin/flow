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
 * Receives the current value of a signal for in-place modification. Used with
 * reference signals to apply changes to mutable values while ensuring
 * dependents are properly notified of the change.
 * <p>
 * This is typically used when you have a mutable object stored in a signal and
 * want to modify it in place rather than replacing it with a new instance.
 *
 * @param <T>
 *            the value type
 */
@FunctionalInterface
public interface ValueModifier<T> {
    /**
     * Modifies the provided value in place.
     *
     * @param value
     *            the value to modify, may be <code>null</code>
     */
    void modify(T value);
}
