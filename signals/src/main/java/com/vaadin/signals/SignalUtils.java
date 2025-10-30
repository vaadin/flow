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

import com.vaadin.signals.impl.SignalTree;

/**
 * Utility class for accessing/using signals internal API.
 * <p>
 * <strong>Note</strong>: This is internal API for Vaadin platform's internal
 * usages. It is not intended for public use and may change or be removed in
 * future releases.
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
    public static SignalTree treeOf(AbstractSignal<?> signal) {
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
    public static boolean isValid(AbstractSignal<?> signal,
            SignalCommand command) {
        return signal.isValid(command);
    }
}
