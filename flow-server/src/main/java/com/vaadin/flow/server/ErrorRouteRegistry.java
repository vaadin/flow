/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.Optional;

import com.vaadin.flow.router.internal.ErrorTargetEntry;

/**
 * Interface class for RouteRegistries that can be used to request for error
 * navigation views for Exceptions.
 *
 * @since
 */
public interface ErrorRouteRegistry extends Serializable {
    /**
     * Get a registered navigation target for given exception. First we will
     * search for a matching cause for in the exception chain and if no match
     * found search by extended type.
     *
     * @param exception
     *            exception to search error view for
     * @return optional error target entry corresponding to the given exception
     */
    Optional<ErrorTargetEntry> getErrorNavigationTarget(Exception exception);
}
