/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.frontend.FrontendUtils;

/**
 * Interface for providing client side routes.
 *
 * @deprecated Provider is deprecated, use
 *             {@link FrontendUtils#getClientRoutes()} instead.
 */
@Deprecated(forRemoval = true)
public interface ClientRoutesProvider extends Serializable {

    /**
     * Get a list of client side routes.
     *
     * @return a list of client side routes. Not null.
     */
    List<String> getClientRoutes();
}
