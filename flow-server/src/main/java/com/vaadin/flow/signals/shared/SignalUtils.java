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
package com.vaadin.flow.signals.shared;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.SignalCommand;
import com.vaadin.flow.signals.shared.impl.SignalTree;

/**
 * Utility class for accessing/using signals internal API.
 * <p>
 * <strong>Note</strong>: This is internal API for Vaadin platform's internal
 * usages. It is not intended for public use and may change or be removed in
 * future releases.
 * 
 * @since 25.1
 */
public class SignalUtils {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private SignalUtils() {
    }

    /**
     * Returns the underlying <code>SignalTree</code> instance of the given
     * signal.
     *
     * @param signal
     *            the signal to get the tree of, not <code>null</code>
     * @return the signal tree instance, not <code>null</code>
     */
    public static SignalTree treeOf(AbstractSharedSignal<?> signal) {
        return signal.tree();
    }

    /**
     * Checks whether the given command is considered valid by the validator
     * instance of the provided signal. In case of composite commands such as
     * transactions, this method will recursively check the validity of all
     * commands in the transaction.
     * <p>
     * <strong>Note</strong>: This only checks the validity of the commands that
     * might make changes to the data.
     *
     * @param signal
     *            the signal to check the command against, not<code>null</code>
     * @param command
     *            the command to check, not <code>null</code>
     * @return <code>true</code> if the command is valid, <code>false</code>
     *         otherwise
     */
    public static boolean isValid(AbstractSharedSignal<?> signal,
            SignalCommand command) {
        return signal.isValid(command);
    }

    /**
     * Gets the type that values of the given signal are read as, if the signal
     * declares one. Only signals that store their value as JSON declare a value
     * type, which means that <code>null</code> is returned for e.g. local
     * signals and computed signals.
     *
     * @param signal
     *            the signal to get the value type for, not <code>null</code>
     * @return the raw value type of the signal, or <code>null</code> if the
     *         signal doesn't declare a value type
     */
    public static @Nullable Class<?> valueTypeOf(Signal<?> signal) {
        Objects.requireNonNull(signal);

        if (signal instanceof SharedValueSignal<?> valueSignal) {
            return valueSignal.valueType().getRawClass();
        } else {
            return null;
        }
    }
}
