/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;

public class TestAbstractRouteRegistry extends AbstractRouteRegistry {

    @Override
    public VaadinContext getContext() {
        return new MockVaadinContext();
    }

}
