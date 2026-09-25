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
package com.vaadin.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The service resolves the pieces Vaadin needs from the CDI container, and a
 * misconfigured container has to be reported as such: a bare CDI exception
 * surfacing out of service initialization says nothing about what is wrong with
 * the application.
 */
class QuarkusVaadinServletServiceTest {

    private BeanManager beanManager;
    private QuarkusVaadinServlet servlet;
    private QuarkusVaadinServletService service;

    @BeforeEach
    void setUp() {
        beanManager = Mockito.mock(BeanManager.class);
        servlet = Mockito.mock(QuarkusVaadinServlet.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        service = new QuarkusVaadinServletService(servlet, configuration,
                beanManager);
    }

    @Test
    void getServlet_isTheQuarkusServletItWasBuiltWith() {
        assertSame(servlet, service.getServlet());
    }

    @Test
    void loadInstantiators_noFactoryBean_reportsAServiceException() {
        Mockito.when(beanManager.getBeans(Mockito.eq(InstantiatorFactory.class),
                Mockito.any())).thenReturn(Set.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.loadInstantiators());
        assertTrue(
                exception.getMessage().contains("no CDI instantiator factory"),
                "the message has to say what is missing: "
                        + exception.getMessage());
    }

    @Test
    void loadInstantiators_severalFactoryBeans_reportsAServiceException() {
        Bean<?> bean = Mockito.mock(Bean.class);
        Mockito.when(beanManager.getBeans(Mockito.eq(InstantiatorFactory.class),
                Mockito.any())).thenReturn(Set.of(bean));
        AmbiguousResolutionException cause = new AmbiguousResolutionException(
                "two of them");
        Mockito.when(beanManager.resolve(Mockito.any())).thenThrow(cause);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.loadInstantiators());
        assertSame(cause, exception.getCause(),
                "the resolution failure has to survive the wrapping");
    }

    @Test
    void loadInstantiators_factoryProducesNothing_reportsAServiceException() {
        InstantiatorFactory factory = Mockito.mock(InstantiatorFactory.class);
        Mockito.when(factory.createInstantitor(service)).thenReturn(null);
        mockFactoryBean(factory);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.loadInstantiators());
        assertTrue(exception.getMessage().contains("Instantiator is null"),
                "the message has to name what came back empty: "
                        + exception.getMessage());
    }

    @Test
    void loadInstantiators_factoryPresent_returnsWhatItProduced()
            throws ServiceException {
        Instantiator instantiator = Mockito.mock(Instantiator.class);
        InstantiatorFactory factory = Mockito.mock(InstantiatorFactory.class);
        Mockito.when(factory.createInstantitor(service))
                .thenReturn(instantiator);
        mockFactoryBean(factory);

        assertEquals(Optional.of(instantiator), service.loadInstantiators());
    }

    @Test
    void lookup_severalBeans_reportsAServiceException() {
        Mockito.when(
                beanManager.getBeans(Mockito.eq(String.class), Mockito.any()))
                .thenThrow(new AmbiguousResolutionException("two of them"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.lookup(String.class));
        assertTrue(exception.getMessage().contains("String"),
                "the message has to name the type that is ambiguous: "
                        + exception.getMessage());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void mockFactoryBean(InstantiatorFactory factory) {
        Bean bean = Mockito.mock(Bean.class);
        Mockito.when(beanManager.getBeans(Mockito.eq(InstantiatorFactory.class),
                Mockito.any())).thenReturn(Set.of(bean));
        Mockito.when(beanManager.resolve(Mockito.any())).thenReturn(bean);
        CreationalContext creationalContext = Mockito
                .mock(CreationalContext.class);
        Mockito.when(beanManager.createCreationalContext(bean))
                .thenReturn(creationalContext);
        Context context = Mockito.mock(Context.class);
        Mockito.when(beanManager.getContext(ApplicationScoped.class))
                .thenReturn(context);
        Mockito.when(context.get(bean, creationalContext)).thenReturn(factory);
    }
}
