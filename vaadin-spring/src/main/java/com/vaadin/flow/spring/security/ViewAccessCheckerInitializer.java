/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.ViewAccessChecker;

/**
 * Helper for checking access to views.
 *
 * @deprecated ViewAccessChecker has been replaced by
 *             {@link com.vaadin.flow.server.auth.NavigationAccessControl}.
 * @since 18.0
 */
@Deprecated(forRemoval = true, since = "24.3")
public class ViewAccessCheckerInitializer implements VaadinServiceInitListener {

    @Autowired
    private ViewAccessChecker viewAccessChecker;

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        serviceInitEvent.getSource()
                .addUIInitListener(uiInitEvent -> uiInitEvent.getUI()
                        .addBeforeEnterListener(viewAccessChecker));
    }

}
