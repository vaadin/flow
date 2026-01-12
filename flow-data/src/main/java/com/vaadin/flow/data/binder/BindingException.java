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
package com.vaadin.flow.data.binder;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder.Binding;

/**
 * A subclass of {@link RuntimeException} which may be thrown inside
 * {@link Binding} logic to wrap an exception caused by {@link HasValue},
 * validator, converter, etc. behavior.
 *
 * @author Vaadin Ltd
 *
 * @see BindingExceptionHandler
 */
public class BindingException extends RuntimeException {

    /**
     * Constructs a new binding exception with the specified detail message.
     *
     * @param message
     *            the detail message
     */
    public BindingException(String message) {
        super(message);
    }

    /**
     * Constructs a new binding exception with the specified detail message and
     * cause.
     *
     * @param message
     *            the detail message
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <code>null</code> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public BindingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new binding exception with the specified cause and a detail
     * message of <code>(cause==null ? null : cause.toString())</code> (which
     * typically contains the class and detail message of <code>cause</code>).
     *
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A <code>null</code> value is
     *            permitted, and indicates that the cause is nonexistent or
     *            unknown.)
     */
    public BindingException(Throwable cause) {
        super(cause);
    }
}
