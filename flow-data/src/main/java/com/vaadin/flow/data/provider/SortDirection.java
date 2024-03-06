/*
 * Copyright (C) 2000-2024 Vaadin Ltd
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
    ASCENDING {
        @Override
        public SortDirection getOpposite() {
            return DESCENDING;
        }
    },

    /**
     * Descending (e.g. Z-A, 9..1) sort order
     */
    DESCENDING {
        @Override
        public SortDirection getOpposite() {
            return ASCENDING;
        }
    };

    /**
     * Get the sort direction that is the direct opposite to this one.
     *
     * @return a sort direction value
     */
    public abstract SortDirection getOpposite();
}
