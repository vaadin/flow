/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.internal.ReflectTools;

public class MockInstantiator implements Instantiator {

    private VaadinServiceInitListener[] serviceInitListeners;

    public MockInstantiator(VaadinServiceInitListener... serviceInitListeners) {
        this.serviceInitListeners = serviceInitListeners;
    }

    @Override
    public boolean init(VaadinService service) {
        return true;
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        return Stream.of(serviceInitListeners);
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        return ReflectTools.createInstance(type);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return ReflectTools.createInstance(componentClass);
    }
}
