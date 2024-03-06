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

import com.vaadin.flow.router.internal.ResolveRequest;

/**
 * Callback interface for resolving {@link ResolveRequest}s to new
 * {@link NavigationState}s.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
@FunctionalInterface
public interface RouteResolver extends Serializable {

    /**
     * Resolves the given request to a new NavigationState.
     *
     * @param request
     *            the request to resolve
     * @return the newly resolved navigation state instance
     */
    NavigationState resolve(ResolveRequest request);
}
