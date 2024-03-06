/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider.hierarchy;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.data.provider.ArrayUpdater;
import com.vaadin.flow.internal.JsonCodec;

import elemental.json.JsonValue;

/**
 * Hierarchical array update strategy aware class.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.1
 *
 */
public interface HierarchicalArrayUpdater extends ArrayUpdater {

    /**
     * Array updater strategy that is aware of hierarchical changes.
     */
    public interface HierarchicalUpdate extends Update {

        /**
         * Clears {@code length} elements in array from the {@code start}
         * position.
         *
         * @param start
         *            the start index
         * @param length
         *            the number of elements to clear
         * @param parentKey
         *            Parent item key that cleared range affects
         */
        void clear(int start, int length, String parentKey);

        /**
         * Sets the {@code items} at the {@code start} position.
         *
         * @param start
         *            the start index
         * @param items
         *            the items to set
         * @param parentKey
         *            Parent item key where given items belongs to
         */
        void set(int start, List<JsonValue> items, String parentKey);

        /**
         * Commits enqueued function calls added via
         * {@link #enqueue(String, Serializable...)}.
         */
        void commit();

        /**
         * Enqueue function call with the given arguments.
         *
         * @see JsonCodec JsonCodec for supported argument types
         * @param name
         *            the name of the function to call, may contain dots to
         *            indicate a function on a property.
         * @param arguments
         *            the arguments to pass to the function. Must be of a type
         *            supported by the communication mechanism, as defined by
         *            {@link JsonCodec}
         */
        void enqueue(String name, Serializable... arguments);

        /**
         * Commits changes for the given {@code updateId} and parent key.
         *
         * @param updateId
         *            the update identifier of the commit for the target
         *            parentKey
         * @param parentKey
         *            target parent key
         * @param levelSize
         *            Total number of direct child items for the given parent
         *            key
         */
        void commit(int updateId, String parentKey, int levelSize);

    }

    @Override
    HierarchicalUpdate startUpdate(int sizeChange);

}
