/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
