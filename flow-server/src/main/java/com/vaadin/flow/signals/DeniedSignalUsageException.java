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
 * Thrown when a signal value is read in a context where signal usage is not
 * allowed. For example, bean-level validators in a {@code Binder} cannot use
 * signals since they are not run inside a reactive effect.
 */
public class DeniedSignalUsageException extends IllegalStateException {
    /**
     * Creates a new exception with the given message.
     *
     * @param message
     *            a message describing the context, not <code>null</code>
     */
    public DeniedSignalUsageException(String message) {
        super(message);
    }
}
