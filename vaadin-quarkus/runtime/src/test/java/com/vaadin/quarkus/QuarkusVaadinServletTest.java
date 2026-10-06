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

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;

/**
 * The servlet publishes the name of the Vaadin servlet handling the current
 * thread, which {@code VaadinServiceScopedContext} needs before a
 * {@code VaadinService} is in {@code CurrentInstance}. What has to hold is that
 * the name is there while the container is inside the servlet and gone the
 * moment it leaves, on that thread and on any other.
 */
@QuarkusTest
public class QuarkusVaadinServletTest {

    @Inject
    BeanManager beanManager;

    private ServletConfig servletConfig;
    private QuarkusVaadinServlet servlet;

    @BeforeEach
    public void setUp() {
        servletConfig = Mockito.mock(ServletConfig.class);
        final ServletContext servletContext = Mockito
                .mock(ServletContext.class);
        Mockito.when(servletContext.getClassLoader())
                .thenReturn(getClass().getClassLoader());

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(Mockito.mock(ResourceProvider.class));
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        final DefaultApplicationConfigurationFactory configurationFactory = Mockito
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
                .thenReturn(configurationFactory);
        Mockito.when(configurationFactory.create(Mockito.any()))
                .thenReturn(applicationConfiguration);
        Mockito.when(lookup.lookup(StaticFileHandlerFactory.class))
                .thenReturn((StaticFileHandlerFactory) StaticFileServer::new);

        Mockito.when(servletConfig.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(servletConfig.getServletContext())
                .thenReturn(servletContext);
        Mockito.when(servletConfig.getServletName()).thenReturn("test-servlet");
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());

        final ServletRegistration servletRegistration = Mockito
                .mock(ServletRegistration.class);
        Mockito.when(servletRegistration.getMappings())
                .thenReturn(Collections.emptyList());
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn((Map) Collections.singletonMap("test-servlet",
                        servletRegistration));

        servlet = new QuarkusVaadinServlet();
        servlet.beanManager = beanManager;
    }

    @AfterEach
    public void tearDown() {
        servlet.destroy();
        VaadinService.setCurrent(null);
    }

    @Test
    public void init_createsQuarkusService_andPublishesTheNameWhileInside()
            throws ServletException {
        AtomicReference<Optional<String>> duringInit = new AtomicReference<>();
        servlet = new QuarkusVaadinServlet() {
            @Override
            protected com.vaadin.flow.server.VaadinServletService createServletService(
                    com.vaadin.flow.function.DeploymentConfiguration configuration)
                    throws com.vaadin.flow.server.ServiceException {
                duringInit.set(QuarkusVaadinServlet.getCurrentServletName());
                return super.createServletService(configuration);
            }
        };
        servlet.beanManager = beanManager;

        servlet.init(servletConfig);

        Assertions.assertEquals(Optional.of("test-servlet"), duringInit.get(),
                "the name has to be readable while the service is being "
                        + "created, which is what needs it");
        Assertions.assertTrue(
                QuarkusVaadinServletService.class
                        .isAssignableFrom(servlet.getService().getClass()),
                "the servlet has to create the Quarkus aware service");
        Assertions.assertEquals(Optional.empty(),
                QuarkusVaadinServlet.getCurrentServletName(),
                "and it has to be gone once init() returns");
    }

    @Test
    public void service_publishesTheNameWhileInside_andClearsItAfter()
            throws Exception {
        servlet.init(servletConfig);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        // A context root without the trailing slash, which VaadinServlet
        // answers with a redirect before it touches the request pipeline.
        Mockito.when(request.getRequestURI()).thenReturn("/app");
        Mockito.when(request.getPathInfo()).thenReturn(null);

        AtomicReference<Optional<String>> duringService = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            duringService.set(QuarkusVaadinServlet.getCurrentServletName());
            return null;
        }).when(response).sendRedirect(Mockito.anyString());

        servlet.service(request, response);

        Mockito.verify(response).sendRedirect("/app/");
        Assertions.assertEquals(Optional.of("test-servlet"),
                duringService.get(),
                "the name has to be readable for the whole request");
        Assertions.assertEquals(Optional.empty(),
                QuarkusVaadinServlet.getCurrentServletName(),
                "and cleared again afterwards, so it cannot leak to the next "
                        + "request on this thread");
    }

    @Test
    public void getCurrentServletName_otherThread_isEmptyNotNull()
            throws Exception {
        servlet.init(servletConfig);

        AtomicReference<Optional<String>> onOtherThread = new AtomicReference<>();
        Thread thread = new Thread(() -> onOtherThread
                .set(QuarkusVaadinServlet.getCurrentServletName()));
        thread.start();
        thread.join();

        Assertions.assertEquals(Optional.empty(), onOtherThread.get(),
                "the name is per thread, and a thread that never entered the "
                        + "servlet has to answer an empty Optional rather than "
                        + "null");
    }
}
