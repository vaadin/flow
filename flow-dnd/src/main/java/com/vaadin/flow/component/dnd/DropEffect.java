/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.dnd;

import java.util.Locale;

/**
 * Used to specify the drop effect to use on dragenter or dragover events.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public enum DropEffect {
    /**
     * A copy of the source item is made at the new location.
     */
    COPY,

    /**
     * An item is moved to a new location.
     */
    MOVE,

    /**
     * A link is established to the source at the new location.
     */
    LINK,

    /**
     * The item may not be dropped.
     */
    NONE;

    /**
     * Parses drop effect from given non-null string.
     *
     * @param string
     *            the string to parse
     * @return the matching drop effect
     */
    static DropEffect fromString(String string) {
        return DropEffect.valueOf(string.toUpperCase(Locale.ENGLISH));
    }
}
