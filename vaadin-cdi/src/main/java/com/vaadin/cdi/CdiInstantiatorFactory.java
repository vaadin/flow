/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.cdi;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import com.vaadin.cdi.annotation.VaadinServiceEnabled;
import com.vaadin.cdi.annotation.VaadinServiceScoped;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.server.VaadinService;

/**
 * Default CDI instantiator factory.
 * <p>
 * Can be overridden by a @{@link VaadinServiceEnabled} CDI
 * Alternative/Specializes, or can be customized with a Decorator.
 *
 * @see Instantiator
 */
@VaadinServiceScoped
@VaadinServiceEnabled
public class CdiInstantiatorFactory implements InstantiatorFactory {

    @Inject
    private BeanManager beanManager;

    @Override
    public Instantiator createInstantitor(VaadinService service) {
        if (!getServiceClass().isAssignableFrom(service.getClass())) {
            return null;
        }

        return new CdiInstantiator(beanManager, service);
    }

    public Class<? extends VaadinService> getServiceClass() {
        return CdiVaadinServletService.class;
    }

}
