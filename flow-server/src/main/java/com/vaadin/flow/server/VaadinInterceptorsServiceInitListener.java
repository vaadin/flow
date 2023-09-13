/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.util.Collections;

/**
 * Sets all Vaadin interceptors on an {@link ServiceInitEvent}.
 *
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see ServiceInitEvent
 * @see VaadinService#init()
 * @author Marcin Grzejszczak
 * @since 24.2
 */
public class VaadinInterceptorsServiceInitListener
        implements VaadinServiceInitListener {

    private final Iterable<VaadinRequestInterceptor> requestInterceptors;
    private final Iterable<VaadinCommandInterceptor> commandInterceptors;

    /**
     * For DI based mechanisms like e.g. Spring.
     *
     * @param requestInterceptors
     *            request interceptors
     * @param commandInterceptors
     *            command interceptors
     */
    public VaadinInterceptorsServiceInitListener(
            Iterable<VaadinRequestInterceptor> requestInterceptors,
            Iterable<VaadinCommandInterceptor> commandInterceptors) {
        this.requestInterceptors = requestInterceptors;
        this.commandInterceptors = commandInterceptors;
    }

    /**
     * For {@link java.util.ServiceLoader} mechanisms.
     *
     * @see com.vaadin.flow.di.DefaultInstantiator
     */
    public VaadinInterceptorsServiceInitListener() {
        this.requestInterceptors = VaadinService.getCurrent() != null
                ? VaadinService.getCurrent().getInstantiator()
                        .getAll(VaadinRequestInterceptor.class).toList()
                : Collections.emptyList();
        this.commandInterceptors = VaadinService.getCurrent() != null
                ? VaadinService.getCurrent().getInstantiator()
                        .getAll(VaadinCommandInterceptor.class).toList()
                : Collections.emptyList();
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        requestInterceptors.forEach(event::addVaadinRequestInterceptor);
        commandInterceptors.forEach(event::addVaadinCommandInterceptor);
    }

}
