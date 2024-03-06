/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.service;

import com.vaadin.flow.server.VaadinService;

public class JavaNonUniqueSPIInstantiator extends JavaSPIInstantiator {

    @Override
    public boolean init(VaadinService service) {
        return "bar".equals(service.getDeploymentConfiguration()
                .getInitParameters().getProperty(FOO));
    }

}
