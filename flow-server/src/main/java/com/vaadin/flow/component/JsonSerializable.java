/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.io.Serializable;

import elemental.json.JsonObject;

/**
 * Base interface for classes that are serializable to and from
 * {@link JsonObject}.
 *
 * @since 1.0
 */
public interface JsonSerializable extends Serializable {

    /**
     * Converts this object to its JSON format.
     *
     * @return the JSON representation of the object
     */
    JsonObject toJson();

    /**
     * Sets the JSON object data into the Java object.
     *
     * @param value
     *            the JSON representation of the object
     * @return this instance, for method chaining
     */
    JsonSerializable readJson(JsonObject value);

}
