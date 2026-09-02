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
package com.vaadin.flow.signals;

/**
 * Thrown when a value is written to a signal that cannot hold a value of that
 * type. This can happen when the value type has been erased from the generic
 * signature of the code that writes the value, which means that the value type
 * cannot be enforced by the compiler.
 *
 * @since 25.3
 */
public class InvalidSignalValueTypeException extends IllegalArgumentException {
    /**
     * Creates a new exception with the given message.
     *
     * @param message
     *            a message describing the invalid value, not <code>null</code>
     */
    public InvalidSignalValueTypeException(String message) {
        super(message);
    }
}
