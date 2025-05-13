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

import java.util.Collections;
import java.util.List;

import jakarta.servlet.ServletException;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Router;
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
            MockVaadinServlet servlet = (MockVaadinServlet) getServlet();
            servlet.service = this;
            if (getServlet().getServletConfig() == null) {
                getServlet().init(new MockServletConfig());
            }
            super.init();
        } catch (ServiceException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

    public void setConfiguration(DeploymentConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public DeploymentConfiguration getDeploymentConfiguration() {
        return configuration != null ? configuration
                : super.getDeploymentConfiguration();
    }
}
