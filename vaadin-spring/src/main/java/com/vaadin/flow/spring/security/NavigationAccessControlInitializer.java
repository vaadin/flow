/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.NavigationAccessControl;

/**
 * Helper to register navigation access control.
 *
 * @see NavigationAccessControl
 * @since 24.3
 */
public class NavigationAccessControlInitializer
        implements VaadinServiceInitListener {

    private final NavigationAccessControl accessControl;

    public NavigationAccessControlInitializer(
            NavigationAccessControl accessControl) {
        this.accessControl = accessControl;
    }

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        serviceInitEvent.getSource()
                .addUIInitListener(uiInitEvent -> uiInitEvent.getUI()
                        .addBeforeEnterListener(accessControl));
    }

}
