/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import javax.servlet.ServletException;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.tests.util.MockDeploymentConfiguration;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MockVaadinServletService extends VaadinServletService {

    private Instantiator instantiator;

    private Lookup lookup;

    private ResourceProvider resourceProvider;

    private static class MockVaadinServlet extends VaadinServlet {

        private final DeploymentConfiguration configuration;

        private MockVaadinServlet(DeploymentConfiguration configuration) {
            this.configuration = configuration;
        }

        @Override
        protected DeploymentConfiguration createDeploymentConfiguration()
                throws ServletException {
            return configuration;
        }

    }

    public MockVaadinServletService() {
        this(new MockDeploymentConfiguration());
    }

    public MockVaadinServletService(
            DeploymentConfiguration deploymentConfiguration) {
        super(new MockVaadinServlet(deploymentConfiguration),
                deploymentConfiguration);
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
            getServlet().init(new MockServletConfig());
            super.init();
        } catch (ServiceException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

}
