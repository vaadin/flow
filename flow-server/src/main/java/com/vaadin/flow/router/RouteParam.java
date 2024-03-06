/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import com.vaadin.flow.internal.Pair;

/**
 * Route parameter containing the name and the value used mainly when
 * constructing a {@link RouteParameters} instance.
 */
public class RouteParam extends Pair<String, String> {

    /**
     * Creates a new route parameter.
     *
     * @param name
     *            the name of the parameter.
     * @param value
     *            the value of the parameter.
     */
    public RouteParam(String name, String value) {
        super(name, value);
    }

    /**
     * Gets the name of the parameter.
     *
     * @return the name of the parameter.
     */
    public String getName() {
        return getFirst();
    }

    /**
     * Gets the value of the parameter.
     *
     * @return the value of the parameter.
     */
    public String getValue() {
        return getSecond();
    }

}
