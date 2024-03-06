/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.dnd;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Used to specify the effect that is allowed for a drag operation.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public enum EffectAllowed {
    /**
     * The item may not be dropped.
     */
    NONE("none"),

    /**
     * A copy of the source item may be made at the new location.
     */
    COPY("copy"),

    /**
     * An item may be moved to a new location.
     */
    MOVE("move"),

    /**
     * A link may be established to the source at the new location.
     */
    LINK("link"),

    /**
     * A copy or move operation is permitted.
     */
    COPY_MOVE("copymove"),

    /**
     * A copy or link operation is permitted.
     */
    COPY_LINK("copylink"),

    /**
     * A link or move operation is permitted.
     */
    LINK_MOVE("linkmove"),

    /**
     * All operations are permitted.
     */
    ALL("all"),

    /**
     * Default state, equivalent to ALL.
     */
    UNINITIALIZED("uninitialized");

    private final String clientPropertyValue;

    EffectAllowed(String value) {
        this.clientPropertyValue = value;
    }

    /**
     * Get the lower case string value that is accepted by the client side drag
     * event.
     *
     * @return String clientPropertyValue accepted by the client side drag
     *         event.
     */
    public String getClientPropertyValue() {
        return clientPropertyValue;
    }

    /**
     * Parses effect allowed from the given non-null string or throws an illegal
     * argument exception if fails to parse it.
     *
     * @param string
     *            the string to parse
     * @return the matching effect allowed
     */
    static EffectAllowed fromString(String string) {
        Optional<EffectAllowed> effectAllowed = Stream
                .of(EffectAllowed.values())
                .filter(ea -> ea.getClientPropertyValue()
                        .equalsIgnoreCase(string.replace("_", "")))
                .findFirst();
        return effectAllowed.orElseThrow(() -> new IllegalArgumentException(
                "Could not parse effect allowed from string " + string));
    }
}
