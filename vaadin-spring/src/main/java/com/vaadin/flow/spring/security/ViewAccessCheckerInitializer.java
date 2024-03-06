/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.ViewAccessChecker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Helper for checking access to views.
 */
@Component
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
