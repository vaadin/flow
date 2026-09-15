/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.quarkus.annotation.VaadinServiceEnabled;

/**
 * Instantiator factory implementation based on Quarkus DI feature.
 *
 * Quarkus DI solution (also called ArC) is based on the Contexts and Dependency
 * Injection for Java 2.0 specification, but it is not a full CDI
 * implementation. Only a subset of the CDI features is implemented.
 *
 * See <a href="https://quarkus.io/guides/cdi-reference">Quarkus CDI
 * Reference</a> for further details.
 *
 * @see InstantiatorFactory
 */
@VaadinServiceEnabled
@ApplicationScoped
public class QuarkusInstantiatorFactory implements InstantiatorFactory {

    @Inject
    BeanManager beanManager;

    @Override
    public Instantiator createInstantitor(VaadinService vaadinService) {
        if (!getServiceClass().isAssignableFrom(vaadinService.getClass())) {
            return null;
        }
        DefaultInstantiator delegate = new DefaultInstantiator(vaadinService) {
            @Override
            protected ClassLoader getClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }
        };
        return new QuarkusInstantiator(delegate, beanManager);
    }

    /**
     * Gets the service class that this instantiator factory is supposed to work
     * with.
     *
     * @return the service class this instantiator factory is supposed to work
     *         with.
     */
    public Class<? extends VaadinService> getServiceClass() {
        return QuarkusVaadinServletService.class;
    }

}
