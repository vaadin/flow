/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

/**
 * Describes sorting direction.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public enum SortDirection {

    /**
     * Ascending (e.g. A-Z, 1..9) sort order
     */
    ASCENDING,

    /**
     * Descending (e.g. Z-A, 9..1) sort order
     */
    DESCENDING;

    /**
     * Get the sort direction that is the direct opposite to this one.
     *
     * @return a sort direction value
     */
    public SortDirection getOpposite() {
        return ASCENDING.equals(this) ? DESCENDING : ASCENDING;
    }

    /**
     * Get the short name of the sort direction.
     *
     * @return The shortened representation of the sort direction, either "asc"
     *         or "desc"
     * @since 24.9
     */
    public String getShortName() {
        return ASCENDING.equals(this) ? "asc" : "desc";
    }
}
