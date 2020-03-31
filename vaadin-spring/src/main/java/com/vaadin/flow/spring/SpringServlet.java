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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.mvc.ServletForwardingController;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Spring application context aware Vaadin servlet implementation.
 * <p>
 * This class is not intended to be used directly. It's instantiated
 * automatically by the Spring add-on:
 * <ul>
 * <li>Spring boot does this via {@link SpringBootAutoConfiguration}.
 * <li>In case of using Spring MVC just extends
 * {@link VaadinMVCWebAppInitializer}.
 * </ul>
 *
 * @author Vaadin Ltd
 *
 */
public class SpringServlet extends VaadinServlet {

    /**
     * Property names that are read from the application.properties file
     */
    protected static final List<String> PROPERTY_NAMES = Arrays.asList(
            Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
            Constants.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
            Constants.SERVLET_PARAMETER_CLOSE_IDLE_SESSIONS,
            Constants.SERVLET_PARAMETER_HEARTBEAT_INTERVAL,
            Constants.SERVLET_PARAMETER_SEND_URLS_AS_PARAMETERS,
            Constants.SERVLET_PARAMETER_PUSH_MODE,
            Constants.SERVLET_PARAMETER_PUSH_URL,
            Constants.SERVLET_PARAMETER_SYNC_ID_CHECK,
            Constants.SERVLET_PARAMETER_PUSH_SUSPEND_TIMEOUT_LONGPOLLING,
            Constants.SERVLET_PARAMETER_REQUEST_TIMING,
            Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_ERROR_PATTERN,
            Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_OPTIONS,
            Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_SUCCESS_PATTERN,
            Constants.SERVLET_PARAMETER_DEVMODE_WEBPACK_TIMEOUT,
            Constants.SERVLET_PARAMETER_ENABLE_DEV_SERVER,
            Constants.SERVLET_PARAMETER_JSBUNDLE,
            Constants.SERVLET_PARAMETER_POLYFILLS,
            Constants.SERVLET_PARAMETER_STATISTICS_JSON,

            Constants.SERVLET_PARAMETER_BROTLI, Constants.I18N_PROVIDER,
            Constants.DISABLE_AUTOMATIC_SERVLET_REGISTRATION,
            Constants.SERVLET_PARAMETER_ENABLE_PNPM,
            Constants.REQUIRE_HOME_NODE_EXECUTABLE,
            Constants.SERVLET_PARAMETER_MAX_MESSAGE_SUSPEND_TIMEOUT,
            Constants.SERVLET_PARAMETER_DEVMODE_ENABLE_LIVE_RELOAD,
            VaadinSession.UI_PARAMETER);

    private final ApplicationContext context;
    private final boolean forwardingEnforced;

    /**
     * Creates a new Vaadin servlet instance with the application
     * {@code context} provided.
     * <p>
     * Use {@code true} as a value for {@code forwardingEnforced} parameter if
     * your servlet is mapped to the root ({@code "/*"}). In the case of root
     * mapping a {@link RootMappedCondition} is checked and
     * {@link VaadinServletConfiguration} is applied conditionally. This
     * configuration provide a {@link ServletForwardingController} so that other
     * Spring endpoints may co-exist with Vaadin application (it's required
     * since root mapping handles any request to the context). This is not
     * needed if you are using non-root mapping since are you free to use the
     * mapping which doesn't overlap with any endpoint mapping. In this case use
     * {@code false} for the {@code forwardingEnforced} parameter.
     *
     *
     * @param context
     *            the Spring application context
     * @param forwardingEnforced
     *            the incoming HttpServletRequest is wrapped in
     *            ForwardingRequestWrapper if {@code true}
     */
    public SpringServlet(ApplicationContext context,
            boolean forwardingEnforced) {
        this.context = context;
        this.forwardingEnforced = forwardingEnforced;
    }

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        super.service(wrapRequest(request), response);
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

    private HttpServletRequest wrapRequest(HttpServletRequest request) {
        if (forwardingEnforced && request.getPathInfo() == null) {
            /*
             * We need to apply a workaround in case of forwarding
             *
             * see https://jira.spring.io/browse/SPR-17457
             */
            return new ForwardingRequestWrapper(request);
        }
        return request;
    }

    private void config(Properties properties) {
        setProperties(PROPERTY_NAMES, properties);
    }

    private void setProperties(List<String> propertyNames,
            Properties properties) {
        propertyNames.stream()
                .forEach(property -> setProperty(property, properties));
    }

    private void setProperty(String property, Properties properties) {
        setProperty("vaadin." + property, property, properties);
    }

    private void setProperty(String envProperty, String initParam,
            Properties properties) {
        Environment env = context.getBean(Environment.class);
        String value = env.getProperty(envProperty);
        if (value != null) {
            properties.put(initParam, value);
        }
    }

}
