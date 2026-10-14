/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.service;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyVaadinServiceInitListener implements VaadinServiceInitListener {

    private final List<VaadinRequestInterceptor> interceptors;

    public MyVaadinServiceInitListener(
            @Autowired List<VaadinRequestInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        interceptors.forEach(event::addVaadinRequestInterceptor);
    }
}
