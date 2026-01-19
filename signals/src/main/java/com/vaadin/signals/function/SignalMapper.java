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
 * Transforms a signal value into another value, creating a derived signal. The
 * transformation is applied every time the signal value is read.
 * <p>
 * The mapper can access other signal values during transformation, making the
 * resulting signal depend on those signals as well.
 *
 * @param <T>
 *            the input signal type
 * @param <R>
 *            the output signal type
 * @see Signal#map(SignalMapper)
 */
@FunctionalInterface
public interface SignalMapper<T, R> {
    /**
     * Applies this mapper to transform a signal value.
     *
     * @param value
     *            the input value, may be <code>null</code>
     * @return the transformed value, may be <code>null</code>
     */
    R map(T value);
}
