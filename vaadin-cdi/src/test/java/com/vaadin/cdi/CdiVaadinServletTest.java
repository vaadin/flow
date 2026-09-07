/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.cdi.context.ServiceUnderTestContext;
import com.vaadin.cdi.util.BeanProvider;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;

public class CdiVaadinServletTest extends AbstractWeldTest {

    @Inject
    private BeanManager beanManager;

    private CdiVaadinServlet servlet;

    @BeforeEach
    public void setUp() throws ServletException {
        final ServletConfig servletConfig = Mockito.mock(ServletConfig.class);
        final ServletContext servletContext = Mockito
                .mock(ServletContext.class);
        ClassLoader loader = CdiVaadinServletServiceTest.class.getClassLoader();
        Mockito.when(servletContext.getClassLoader()).thenReturn(loader);

        Lookup lookup = Mockito.mock(Lookup.class);
        ResourceProvider provider = Mockito.mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(provider);
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        final DefaultApplicationConfigurationFactory applicationConfigurationFactory = Mockito
                .mock(DefaultApplicationConfigurationFactory.class);
        final ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(applicationConfiguration.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        final VaadinContext context = Mockito.mock(VaadinContext.class);
        Mockito.when(applicationConfiguration.getContext()).thenReturn(context);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        Mockito.when(context.getAttribute(ArgumentMatchers.any(Class.class),
                ArgumentMatchers.any(Supplier.class)))
                .then(i -> i.getArgument(1, Supplier.class).get());

        Mockito.when(lookup.lookup(ApplicationConfigurationFactory.class))
                .thenReturn(applicationConfigurationFactory);
        Mockito.when(applicationConfigurationFactory.create(Mockito.any()))
                .thenReturn(applicationConfiguration);

        StaticFileHandlerFactory staticFileHandlerFactory = vaadinService -> new StaticFileServer(
                vaadinService);
        Mockito.when(lookup.lookup(StaticFileHandlerFactory.class))
                .thenReturn(staticFileHandlerFactory);

        Mockito.when(servletConfig.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(servletConfig.getServletContext())
                .thenReturn(servletContext);
        Mockito.when(servletConfig.getServletName()).thenReturn("test");
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        final ServletRegistration servletRegistration = Mockito
                .mock(ServletRegistration.class);
        final Map servletRegistrationMap = Collections.singletonMap("test",
                servletRegistration);
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn(servletRegistrationMap);
        Mockito.when(servletRegistration.getMappings())
                .thenReturn(Collections.emptyList());

        servlet = new CdiVaadinServlet();
        BeanProvider.injectFields(servlet);
        servlet.init(servletConfig);
    }

    @AfterEach
    public void tearDown() {
        new ServiceUnderTestContext(beanManager).tearDownAll();
        servlet.destroy();
    }

    @Test
    public void getService_servletInitialized_cdiVaadinServletServiceReturned() {
        final VaadinService service = servlet.getService();
        Assertions.assertTrue(CdiVaadinServletService.class
                .isAssignableFrom(service.getClass()));
    }
}
