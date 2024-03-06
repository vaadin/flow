/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.model;

import com.vaadin.client.flow.StateNode;
import com.vaadin.client.flow.binding.SimpleElementBindingStrategy;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;

/**
 * The storage class for set of updatable model properties.
 * <p>
 * This class is stored inside a {@link StateNode} via
 * {@link StateNode#setNodeData(Object)} if there is any data to store at all.
 * Once it's stored in the {@link StateNode} the code which sends updates to the
 * server side when a polymer property is updated uses this data to detect
 * whether server expects the update to be sent(see
 * {@link SimpleElementBindingStrategy}).
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class UpdatableModelProperties {

    private final JsSet<String> properties = JsCollections.set();

    /**
     * Creates a new instance of storage class based on given
     * {@code properties}.
     *
     * @param properties
     *            updatable properties array
     */
    public UpdatableModelProperties(JsArray<String> properties) {
        properties.forEach(this.properties::add);
    }

    /**
     * Tests whether the {@code property} is updatable.
     *
     * @param property
     *            the property to test
     * @return {@code true} if property is updatable
     */
    public boolean isUpdatableProperty(String property) {
        return properties.has(property);
    }
}
