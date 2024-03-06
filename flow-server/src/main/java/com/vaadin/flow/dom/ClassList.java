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
 * Representation of the class names for an {@link Element}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ClassList extends Set<String>, Serializable {

    /**
     * Sets or removes the given class name, based on the {@code set} parameter.
     *
     * @param className
     *            the class name to set or remove
     * @param set
     *            true to set the class name, false to remove it
     * @return true if the class list was modified (class name added or
     *         removed), false otherwise
     */
    default boolean set(String className, boolean set) {
        if (set) {
            return add(className);
        } else {
            return remove(className);
        }
    }

}
