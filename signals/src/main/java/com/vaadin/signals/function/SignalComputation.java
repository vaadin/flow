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

import com.vaadin.signals.Signal;

/**
 * Computes a signal value based on other signals. The computation is
 * automatically re-run when any dependent signal changes.
 * <p>
 * Dependencies are automatically tracked - any signal whose value is accessed
 * during the computation becomes a dependency. The computation is lazy and only
 * runs when the signal value is accessed and the previous value might have been
 * invalidated by dependent signal changes.
 *
 * @param <T>
 *            the computed value type
 * @see Signal#computed(SignalComputation)
 */
@FunctionalInterface
public interface SignalComputation<T> {
    /**
     * Computes the signal value, automatically tracking dependencies on other
     * signals.
     *
     * @return the computed value, may be <code>null</code>
     */
    T compute();
}
