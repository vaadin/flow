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
package com.vaadin.flow.di;

import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.vaadin.router.event.NavigationEvent;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServiceInitListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasElement;
import com.vaadin.util.ReflectTools;

/**
 * Default instantiator that is used if no other instantiator has been
 * registered. This implementation uses vanilla Java mechanisms such as
 * {@link Class#newInstance()} and {@link ServiceLoader}.
 *
 * @author Vaadin Ltd
 */
public class DefaultInstantiator implements Instantiator {
    private VaadinService service;

    /**
     * Creates a new instantiator for the given service.
     *
     * @param service
     *            the service to use
     */
    public DefaultInstantiator(VaadinService service) {
        this.service = service;
    }

    @Override
    public boolean init(VaadinService service) {
        return service == this.service;
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        return getServiceLoaderListeners(service.getClassLoader());
    }

    /**
     * Helper for finding service init listeners using {@link ServiceLoader}.
     *
     * @param classloader
     *            the classloader to use for finding the listeners
     * @return a stream of service init listeners
     */
    public static Stream<VaadinServiceInitListener> getServiceLoaderListeners(
            ClassLoader classloader) {
        ServiceLoader<VaadinServiceInitListener> loader = ServiceLoader
                .load(VaadinServiceInitListener.class, classloader);
        return StreamSupport.stream(loader.spliterator(), false);
    }

    @Override
    public <T extends HasElement> T createRouteTarget(Class<T> routeTargetType,
            NavigationEvent event) {
        return ReflectTools.createInstance(routeTargetType);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return ReflectTools.createInstance(componentClass);
    }
}
