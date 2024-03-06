/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.io.Serializable;

/**
 * Generic class representing an immutable pair of values.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Pair<U extends Serializable, V extends Serializable>
        implements Serializable {

    private final U first;
    private final V second;

    /**
     * Creates a new pair.
     *
     * @param u
     *            the value of the first component
     * @param v
     *            the value of the second component
     */
    public Pair(U u, V v) {
        first = u;
        second = v;
    }

    /**
     * Gets the first component of the pair.
     *
     * @return the first component of the pair
     */
    public U getFirst() {
        return first;
    }

    /**
     * Gets the second component of the pair.
     *
     * @return the second component of the pair
     */
    public V getSecond() {
        return second;
    }

}
