/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.io.Serializable;

/**
 * Defines route parameters for navigation targets for use in routing.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            type parameter type
 */
@FunctionalInterface
public interface HasUrlParameter<T> extends Serializable {

    /**
     * Notifies about navigating to the target that implements this interface.
     *
     * @param event
     *            the navigation event that caused the call to this method
     * @param parameter
     *            the resolved url parameter
     */
    void setParameter(BeforeEvent event, T parameter);
}
