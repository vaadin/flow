/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.Set;

/**
 * Representation of the theme names for an {@link Element}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ThemeList extends Set<String>, Serializable {

    /**
     * Sets or removes the given theme name, based on the {@code set} parameter.
     *
     * @param themeName
     *            the theme name to set or remove
     * @param set
     *            true to set the theme name, false to remove it
     * @return true if the theme list was modified (theme name added or
     *         removed), false otherwise
     */
    default boolean set(String themeName, boolean set) {
        return set ? add(themeName) : remove(themeName);
    }
}
