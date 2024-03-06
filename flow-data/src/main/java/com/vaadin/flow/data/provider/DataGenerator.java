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

import elemental.json.JsonObject;

/**
 * A data generator for {@link DataCommunicator}. Used to inject custom data to
 * data items sent to the client for extension purposes.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            the data type
 */
@FunctionalInterface
public interface DataGenerator<T> extends Serializable {

    /**
     * Adds custom data for the given item to its serialized {@code JsonObject}
     * representation. This JSON object will be sent to client-side
     * DataProvider.
     *
     * @param item
     *            the data item being serialized
     * @param jsonObject
     *            the JSON object being sent to the client
     */
    void generateData(T item, JsonObject jsonObject);

    /**
     * Informs the {@code DataGenerator} that the given data item has been
     * dropped and is no longer needed. This method should clean up any unneeded
     * information stored for this item.
     *
     * @param item
     *            the dropped data item
     */
    default void destroyData(T item) {
    }

    /**
     * Informs the {@code DataGenerator} that all data has been dropped. This
     * method should clean up any unneeded information stored for items.
     */
    default void destroyAllData() {
    }

    /**
     * Informs the {@code DataGenerator} that a data object has been updated.
     * This method should update any unneeded information stored for given item.
     *
     * @param item
     *            the updated item
     */
    default void refreshData(T item) {
    }
}
