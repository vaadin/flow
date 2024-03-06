/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * Route registry with a public constructor for testing purposes.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class TestRouteRegistry extends ApplicationRouteRegistry {
    /**
     * Creates a new test route registry.
     */
    public TestRouteRegistry() {
        super(new MockVaadinContext(new DefaultRoutePathProvider()));
    }

    public TestRouteRegistry(RoutePathProvider provider) {
        super(new MockVaadinContext(provider));
    }
}
