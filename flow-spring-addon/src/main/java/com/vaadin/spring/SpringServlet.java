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
package com.vaadin.spring;

import java.util.Properties;

import org.springframework.context.ApplicationContext;

import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.Constants;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

/**
 * Spring application context aware Vaadin servlet implementation.
 *
 * @author Vaadin Ltd
 *
 */
public class SpringServlet extends VaadinServlet {

    private ApplicationContext context;

    /**
     * Creates a new Vaadin servlet instance with the application
     * {@code context} provided.
     *
     * @param the
     *            Spring application context
     */
    public SpringServlet(ApplicationContext context) {
        this.context = context;
    }

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        SpringVaadinServletService service = new SpringVaadinServletService(
                this, deploymentConfiguration, context);
        service.init();
        return service;
    }

    @Override
    protected DeploymentConfiguration createDeploymentConfiguration(
            Properties initParameters) {
        Properties properties = new Properties();
        properties.put(Constants.SERVLET_PARAMETER_USING_NEW_ROUTING,
                Boolean.TRUE.toString());
        return super.createDeploymentConfiguration(properties);
    }

}
