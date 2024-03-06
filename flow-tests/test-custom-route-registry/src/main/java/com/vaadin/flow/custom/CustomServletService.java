/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.custom;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

/**
 * ServletService that returns a CustomRouteRegistry for the RouteRegistry.
 */
public class CustomServletService extends VaadinServletService {

    public CustomServletService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration) {
        super(servlet, deploymentConfiguration);
    }

    @Override
    protected RouteRegistry getRouteRegistry() {
        return CustomRouteRegistry.getInstance(getContext());
    }
}
