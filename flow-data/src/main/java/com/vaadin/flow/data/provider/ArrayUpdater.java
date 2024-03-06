/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.io.Serializable;
import java.util.List;

import elemental.json.JsonValue;

/**
 * Array update strategy aware class.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface ArrayUpdater extends Serializable {

    /**
     * Array updater strategy.
     *
     */
    public interface Update extends Serializable {
        /**
         * Clears {@code length} elements in array from the {@code start}
         * position.
         *
         * @param start
         *            the start index
         * @param length
         *            the number of elements to clear
         */
        void clear(int start, int length);

        /**
         * Sets the {@code items} at the {@code start} position.
         *
         * @param start
         *            the start index
         * @param items
         *            the items to set
         */
        void set(int start, List<JsonValue> items);

        /**
         * Commits changes for the given {@code updateId}.
         *
         * @param updateId
         *            the update identifier of the commit
         */
        void commit(int updateId);
    }

    /**
     * Starts update of an array.
     *
     * @param sizeChange
     *            the size of the array where changes happened
     * @return array update strategy
     */
    Update startUpdate(int sizeChange);

    /**
     * Initialize the array.
     */
    void initialize();
}
