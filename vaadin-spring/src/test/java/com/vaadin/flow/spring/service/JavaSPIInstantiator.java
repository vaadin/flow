/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.service;

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class JavaSPIInstantiator implements Instantiator {

    static final String FOO = "foo";

    @Override
    public boolean init(VaadinService service) {
        return Boolean.FALSE.toString()
                .equals(service.getDeploymentConfiguration().getInitParameters()
                        .getProperty(FOO));
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        return Stream.of();
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        return null;
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return null;
    }
}
