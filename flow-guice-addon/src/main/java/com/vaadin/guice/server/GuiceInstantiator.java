/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.guice.server;

import com.google.common.collect.Streams;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.router.event.NavigationEvent;
import com.vaadin.server.VaadinServiceInitListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasElement;

import java.util.stream.Stream;

/**
 * Default Guice instantiator that is used if no other instantiator has been
 * registered.
 *
 * @author Vaadin Ltd
 */
class GuiceInstantiator extends DefaultInstantiator {

    private final GuiceVaadinServlet servlet;

    /**
     * Creates a new spring instantiator instance.
     *
     * @param service the service to use
     */
    public GuiceInstantiator(GuiceVaadinServletService service) {
        super(service);

        servlet = (GuiceVaadinServlet) service.getServlet();
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        Stream<VaadinServiceInitListener> guiceListeners =
                Streams.stream(servlet.getServiceInitListeners());
        return Stream.concat(super.getServiceInitListeners(), guiceListeners);
    }

    @Override
    public <T extends HasElement> T createRouteTarget(Class<T> routeTargetType,
                                                      NavigationEvent event) {
        return servlet.getInjector().getInstance(routeTargetType);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return servlet.getInjector().getInstance(componentClass);
    }
}
