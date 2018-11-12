/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.dom;

import com.vaadin.flow.component.HasEnabled;

/**
 * Controls RPC communication from the client side to the server side respecting
 * enabled state.
 *
 * @see Element#setEnabled(boolean)
 * @see Element#isEnabled()
 * @see HasEnabled#setEnabled(boolean)
 * @see HasEnabled#isEnabled()
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public enum DisabledUpdateMode {
    /**
     * If used then updates from the client side are allowed even for disabled
     * element.
     */
    ALWAYS,
    /**
     * If used then updates from the client side are allowed only if element is
     * enabled.
     */
    ONLY_WHEN_ENABLED;

    /**
     * Gets the most permissive out of two update modes.
     *
     * @param mode1
     *            the first mode, or <code>null</code>
     * @param mode2
     *            the second mode, or <code>null</code>
     * @return the most permissive mode, or <code>null</code> if both parameters
     *         are <code>null</code>
     */
    public static DisabledUpdateMode mostPermissive(DisabledUpdateMode mode1,
            DisabledUpdateMode mode2) {
        if (mode1 == ALWAYS || mode2 == ALWAYS) {
            return ALWAYS;
        } else if (mode1 == ONLY_WHEN_ENABLED || mode2 == ONLY_WHEN_ENABLED) {
            return ONLY_WHEN_ENABLED;
        } else {
            assert mode1 == null && mode2 == null;
            return null;
        }
    }
}
