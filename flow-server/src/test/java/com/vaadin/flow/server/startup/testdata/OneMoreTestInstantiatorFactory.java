/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup.testdata;

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

public class OneMoreTestInstantiatorFactory implements InstantiatorFactory {

    public static class TestInstantiator implements Instantiator {

        @Override
        public boolean init(VaadinService service) {
            return false;
        }

        @Override
        public Stream<VaadinServiceInitListener> getServiceInitListeners() {
            return null;
        }

        @Override
        public <T> T getOrCreate(Class<T> type) {
            return null;
        }

        @Override
        public <T extends Component> T createComponent(
                Class<T> componentClass) {
            return null;
        }

    }

    @Override
    public Instantiator createInstantitor(VaadinService service) {
        return null;
    }

}
