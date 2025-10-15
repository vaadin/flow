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

import jakarta.servlet.ServletException;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.DefaultRoutePathProvider;
import com.vaadin.flow.router.RoutePathProvider;
import com.vaadin.flow.router.Router;
import com.vaadin.signals.SignalEnvironment;
import com.vaadin.tests.util.MockDeploymentConfiguration;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MockVaadinServletService extends VaadinServletService {

    private Instantiator instantiator;

    private Router router;

    private DeploymentConfiguration configuration;

    private Lookup lookup;

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

    public MockVaadinServletService() {
        this(new MockDeploymentConfiguration());
    }

    public MockVaadinServletService(boolean init) {
        this(new MockDeploymentConfiguration(), init);
    }

    public MockVaadinServletService(
            DeploymentConfiguration deploymentConfiguration) {
        this(deploymentConfiguration, true);
    }

    public MockVaadinServletService(
            DeploymentConfiguration deploymentConfiguration, boolean init) {
        super(new MockVaadinServlet(deploymentConfiguration),
                deploymentConfiguration);
        if (init) {
            init();
        }
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    @Override
    public Router getRouter() {
        return router != null ? router : super.getRouter();
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
    public void init() {
        try {
            resetSignalEnvironment();
            MockVaadinServlet servlet = (MockVaadinServlet) getServlet();
            servlet.service = this;
            if (getServlet().getServletConfig() == null) {
                getServlet().init(new MockServletConfig());
            }
            if (lookup == null
                    && getContext().getAttribute(Lookup.class) == null) {
                lookup = Mockito.mock(Lookup.class);
                Mockito.when(lookup.lookup(RoutePathProvider.class))
                        .thenReturn(new DefaultRoutePathProvider());
                instrumentMockLookup(lookup);
                getContext().setAttribute(Lookup.class, lookup);
            }
            super.init();
        } catch (ServiceException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

    protected void instrumentMockLookup(Lookup lookup) {
        // no-op
    }

    public void setConfiguration(DeploymentConfiguration configuration) {
        this.configuration = configuration;
    }

    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public DeploymentConfiguration getDeploymentConfiguration() {
        return configuration != null ? configuration
                : super.getDeploymentConfiguration();
    }

    private void resetSignalEnvironment() {
        try {
            Field environments = SignalEnvironment.class
                    .getDeclaredField("environments");
            environments.setAccessible(true);
            ((List<?>) environments.get(null)).clear();
        } catch (Exception e) {
            throw new AssertionError("Failed to reset Signal environment", e);
        }
    }

    @Override
    protected Executor createDefaultExecutor() {
        Executor executor = super.createDefaultExecutor();
        if (executor instanceof ThreadPoolExecutor threadPoolExecutor) {
            threadPoolExecutor.setCorePoolSize(0);
            threadPoolExecutor.setMaximumPoolSize(4);
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
