/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus.context;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.quarkus.QuarkusVaadinServletService;
import com.vaadin.quarkus.TestQuarkusVaadinServletService;
import com.vaadin.quarkus.context.VaadinServiceScopedContext.ContextualStorageManager;

public class ServiceUnderTestContext implements UnderTestContext {
    private QuarkusVaadinServletService service;
    private static int NDX;
    private final BeanManager beanManager;

    public ServiceUnderTestContext(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    @Override
    public void activate() {
        NDX++;
        service = new TestQuarkusVaadinServletService(beanManager, NDX + "");
        try {
            service.init();
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
        VaadinService.setCurrent(service);
    }

    @Override
    public void tearDownAll() {
        VaadinService.setCurrent(null);
        Context appContext = beanManager.getContext(ApplicationScoped.class);
        Set<Bean<?>> beans = beanManager
                .getBeans(ContextualStorageManager.class);
        ((AlterableContext) appContext).destroy(beanManager.resolve(beans));
    }

    @Override
    public void destroy() {
        if (service != null) {
            beanManager.getEvent().fire(new ServiceDestroyEvent(service));
        }
    }

    public QuarkusVaadinServletService getService() {
        return service;
    }
}
