/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.springframework.stereotype.Component;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.test.store.ProductView;

@Component
public class ApplicationServiceInitListener
        implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {

        if ("true".equalsIgnoreCase(
                System.getProperty("route.hierarchy.enabled", "false"))) {
            System.out.println(
                    "Route hierarchy is enabled. Registering additional route with hierarchical structure like 'catalog/product/0'.");
            RouteConfiguration.forApplicationScope()
                    .setAnnotatedRoute(ProductView.class);
        }
    }
}
