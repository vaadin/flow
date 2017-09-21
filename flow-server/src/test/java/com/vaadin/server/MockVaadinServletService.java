/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server;

import java.util.Collections;
import java.util.List;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.tests.util.MockDeploymentConfiguration;

/**
 *
 * @author Vaadin Ltd
 */
public class MockVaadinServletService extends VaadinServletService {

    private Instantiator instantiator;

    public MockVaadinServletService() {
        this(new MockDeploymentConfiguration());
    }

    public MockVaadinServletService(
            DeploymentConfiguration deploymentConfiguration) {
        this(new VaadinServlet(), deploymentConfiguration);
    }

    public MockVaadinServletService(VaadinServlet servlet,
            DeploymentConfiguration deploymentConfiguration) {
        super(servlet, deploymentConfiguration);
    }

    @Override
    protected List<RequestHandler> createRequestHandlers()
            throws ServiceException {
        return Collections.emptyList();
    }

    public void init(Instantiator instantiator) throws ServiceException {
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

}
