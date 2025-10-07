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
package com.vaadin.flow.server;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mockito.Mockito;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Router;
import org.slf4j.LoggerFactory;

public class MockVaadinServletService extends VaadinServletService {

    private Instantiator instantiator;

    private Router router;

    private ResourceProvider resourceProvider = Mockito
            .mock(ResourceProvider.class);

    private Lookup lookup = Mockito.mock(Lookup.class);

    private static class MockVaadinServlet extends VaadinServlet {

        private final DeploymentConfiguration configuration;

        private VaadinServletService service;

        private MockVaadinServlet(DeploymentConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        protected DeploymentConfiguration createDeploymentConfiguration()
                throws ServletException {
            return configuration;
        }

        @Override
        protected VaadinServletService createServletService(
                DeploymentConfiguration deploymentConfiguration)
                throws ServiceException {
            return service;
        }

    }

    public MockVaadinServletService(
            DeploymentConfiguration deploymentConfiguration) {
        super(new MockVaadinServlet(deploymentConfiguration),
                deploymentConfiguration);
        init();
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    @Override
    public Router getRouter() {
        return router != null ? router : super.getRouter();
    }

    @Override
    protected RouteRegistry getRouteRegistry() {
        return Mockito.mock(RouteRegistry.class);
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        return Collections.emptyList();
    }

    public void init(Instantiator instantiator) {
        this.instantiator = instantiator;

        init();
    }

    @Override
    protected Instantiator createInstantiator() throws ServiceException {
        if (instantiator != null) {
            return instantiator;
        }
        return super.createInstantiator();
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            super.setClassLoader(classLoader);
        }
    }

    @Override
    public void init() {
        try {
            MockVaadinServlet servlet = (MockVaadinServlet) getServlet();
            servlet.service = this;

            if (getServlet().getServletConfig() == null) {
                ServletConfig config = Mockito.mock(ServletConfig.class);
                ServletContext context = Mockito.mock(ServletContext.class);
                Mockito.when(config.getServletContext()).thenReturn(context);

                Mockito.when(lookup.lookup(ResourceProvider.class))
                        .thenReturn(resourceProvider);
                StaticFileHandlerFactory factory = Mockito
                        .mock(StaticFileHandlerFactory.class);
                Mockito.when(lookup.lookup(StaticFileHandlerFactory.class))
                        .thenReturn(factory);

                Mockito.when(context.getAttribute(Lookup.class.getName()))
                        .thenReturn(lookup);
                getServlet().init(config);
            }
            super.init();
        } catch (ServiceException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Executor createDefaultExecutor() {
        Executor executor = super.createDefaultExecutor();
        if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
            threadPoolExecutor.setCorePoolSize(2);
            threadPoolExecutor.setKeepAliveTime(10, TimeUnit.SECONDS);
            ThreadFactory threadFactory = threadPoolExecutor.getThreadFactory();
            threadPoolExecutor.setThreadFactory(r -> {
                Thread thread = threadFactory.newThread(r);
                thread.setUncaughtExceptionHandler((t, e) -> {
                    LoggerFactory.getLogger(getClass()).error(
                            "An uncaught exception occurred in thread {}",
                            t.getName(), e);
                });
                return thread;
            });
        }
        return executor;
    }

}
