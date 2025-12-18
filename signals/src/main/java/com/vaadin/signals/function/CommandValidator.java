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

import java.util.Objects;

import com.vaadin.signals.SignalCommand;

/**
 * Validates whether a signal command is allowed to be executed on a signal.
 * Used to enforce access control, readonly constraints, or custom business
 * rules.
 * <p>
 * Validators can be composed using the {@link #and(CommandValidator)} method
 * to require multiple validation rules to pass.
 */
@FunctionalInterface
public interface CommandValidator {
    /**
     * A validator that accepts all commands without restriction.
     */
    CommandValidator ACCEPT_ALL = command -> true;

    /**
     * A validator that rejects all commands, making the signal readonly.
     */
    CommandValidator REJECT_ALL = command -> false;

    /**
     * Tests whether the given command is valid and should be allowed to
     * execute.
     *
     * @param command
     *            the command to validate, not <code>null</code>
     * @return <code>true</code> if the command is valid, <code>false</code>
     *         otherwise
     */
    boolean isValid(SignalCommand command);

    /**
     * Returns a composed validator that represents a logical AND of this
     * validator and another. Both validators must accept the command for the
     * composed validator to accept it.
     *
     * @param other
     *            the validator to combine with, not <code>null</code>
     * @return a composed validator, not <code>null</code>
     */
    default CommandValidator and(CommandValidator other) {
        Objects.requireNonNull(other);
        return command -> isValid(command) && other.isValid(command);
    }
}
