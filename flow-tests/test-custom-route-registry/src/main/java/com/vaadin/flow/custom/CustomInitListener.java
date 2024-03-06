/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.custom;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Customized VaadinServiceInitListener that registers the CustomRoute to
 * CustomRouteRegistry.
 */
public class CustomInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        final CustomRouteRegistry registry = CustomRouteRegistry
                .getInstance(event.getSource().getContext());

        RouteConfiguration configuration = RouteConfiguration
                .forRegistry(registry);

        configuration.setRoute("", CustomRoute.class);
    }
}
