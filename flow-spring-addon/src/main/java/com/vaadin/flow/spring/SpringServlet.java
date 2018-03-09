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
package com.vaadin.flow.spring;

import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

/**
 * Spring application context aware Vaadin servlet implementation.
 *
 * @author Vaadin Ltd
 *
 */
public class SpringServlet extends VaadinServlet {

    private final ApplicationContext context;

    /**
     * Creates a new Vaadin servlet instance with the application
     * {@code context} provided.
     *
     * @param context
     *            the Spring application context
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
        Properties properties = new Properties(initParameters);
        config(properties);
        return super.createDeploymentConfiguration(properties);
    }

    private void config(Properties properties) {
        setProperty(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, properties);
        setProperty(Constants.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                properties);
        setProperty(Constants.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS,
                properties);
        setProperty(Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL, properties);
        setProperty(Constants.SERVLET_PARAMETER_POLYFILL_BASE, properties);
        setProperty(Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
                properties);
        setProperty(Constants.SERVLET_PARAMETER_PUSH_MODE, properties);
        setProperty(
                Constants.SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING,
                properties);
        setProperty(Constants.SERVLET_PARAMETER_REQUEST_TIMING, properties);

        setProperty(Constants.DISABLE_WEBJARS, properties);
        setProperty(Constants.FRONTEND_URL_ES5, properties);
        setProperty(Constants.FRONTEND_URL_ES6, properties);

        setProperty(Constants.I18N_PROVIDER, properties);

        properties.put(Constants.SERVLET_PARAMETER_USING_NEW_ROUTING,
                Boolean.TRUE.toString());
    }

    private void setProperty(String property, Properties properties) {
        setProperty("vaadin." + property, property, properties);
    }

    private void setProperty(String envProperty, String initParam,
            Properties properties) {
        Environment env = context.getBean(Environment.class);
        String productionMode = env.getProperty(envProperty);
        if (productionMode != null) {
            properties.put(initParam, productionMode);
        }
    }

}
