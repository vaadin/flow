/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.io.Serializable;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Describes a set of properties that can be used for configuration based on
 * property names instead of setter and getter callbacks.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 *
 * @param <T>
 *            the type for which the properties are defined
 */
public interface PropertySet<T> extends Serializable {
    /**
     * Gets all known properties as a stream.
     *
     * @return a stream of property names, not <code>null</code>
     */
    Stream<PropertyDefinition<T, ?>> getProperties();

    /**
     * Gets the definition for the named property, or an empty optional if there
     * is no property with the given name.
     *
     * @param name
     *            the property name to look for, not <code>null</code>
     * @return the property definition, or empty optional if property doesn't
     *         exist
     */
    Optional<PropertyDefinition<T, ?>> getProperty(String name);
}
