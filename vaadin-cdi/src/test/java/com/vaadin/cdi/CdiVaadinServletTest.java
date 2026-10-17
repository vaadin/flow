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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CdiVaadinServletTest extends AbstractWeldTest {

    @Inject
    private BeanManager beanManager;

    private CdiVaadinServlet servlet;

    @BeforeEach
    public void setUp() throws ServletException {
        final ServletConfig servletConfig = mock(ServletConfig.class);
        final ServletContext servletContext = Mockito
                .mock(ServletContext.class);
        ClassLoader loader = CdiVaadinServletServiceTest.class.getClassLoader();
        when(servletContext.getClassLoader()).thenReturn(loader);

        Lookup lookup = mock(Lookup.class);
        ResourceProvider provider = mock(ResourceProvider.class);
        when(lookup.lookup(ResourceProvider.class)).thenReturn(provider);
        when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        final DefaultApplicationConfigurationFactory applicationConfigurationFactory = Mockito
                .mock(DefaultApplicationConfigurationFactory.class);
        final ApplicationConfiguration applicationConfiguration = Mockito
                .mock(ApplicationConfiguration.class);
        when(applicationConfiguration.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        final VaadinContext context = mock(VaadinContext.class);
        when(applicationConfiguration.getContext()).thenReturn(context);
        when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        when(context.getAttribute(any(Class.class), any(Supplier.class)))
                .then(i -> i.getArgument(1, Supplier.class).get());

        when(lookup.lookup(ApplicationConfigurationFactory.class))
                .thenReturn(applicationConfigurationFactory);
        when(applicationConfigurationFactory.create(any()))
                .thenReturn(applicationConfiguration);

        StaticFileHandlerFactory staticFileHandlerFactory = vaadinService -> new StaticFileServer(
                vaadinService);
        when(lookup.lookup(StaticFileHandlerFactory.class))
                .thenReturn(staticFileHandlerFactory);

        when(servletConfig.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletConfig.getServletName()).thenReturn("test");
        when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        final ServletRegistration servletRegistration = Mockito
                .mock(ServletRegistration.class);
        final Map servletRegistrationMap = Collections.singletonMap("test",
                servletRegistration);
        when(servletContext.getServletRegistrations())
                .thenReturn(servletRegistrationMap);
        when(servletRegistration.getMappings())
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
        assertTrue(CdiVaadinServletService.class
                .isAssignableFrom(service.getClass()));
    }
}
