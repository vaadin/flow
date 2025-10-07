/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import org.junit.After;
import org.junit.Before;

import com.vaadin.flow.server.VaadinService;

import static org.junit.Assert.assertNull;

/**
 * Helper for test classes that need to have {@code VaadinService.getCurrent()}
 * populated.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class HasCurrentService {
    // Store the service to prevent it from being garbage collected while the
    // test is running
    private VaadinService service;

    @Before
    public void setUpCurrentService() {
        clearCurrentService();
        assertNull(VaadinService.getCurrent());

        service = createService();
        VaadinService.setCurrent(service);
    }

    protected abstract VaadinService createService();

    @After
    public void clearCurrentService() {
        VaadinService.setCurrent(null);
        service = null;
    }
}
