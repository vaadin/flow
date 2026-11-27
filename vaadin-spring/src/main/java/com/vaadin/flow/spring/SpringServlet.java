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
package com.vaadin.flow.spring;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.mvc.ServletForwardingController;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;

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
 * @since 10.0
 */
public class SpringServlet extends VaadinServlet {

    /**
     * Property names that are read from the application.properties file
     * 
     * @since 12.0.2
     */
    protected static final List<String> PROPERTY_NAMES = Arrays
            .stream(InitParameters.class.getDeclaredFields())
            // thanks to java code coverage which adds non-existent
            // initially variables everywhere: we should skip this extra
            // field
            .filter(field -> !field.isSynthetic()).map(field -> {
                try {
                    return (String) field.get(null);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("unable to access field",
                            e);
                }
            }).collect(Collectors.toList());

    /** The application context the servlet and its service are wired to. */
    private final ApplicationContext context;

    /** Whether the servlet is mapped to the context root. */
    private final boolean rootMapping;

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
     * @param rootMapping
     *            the incoming HttpServletRequest is wrapped in
     *            ForwardingRequestWrapper if {@code true}
     * @since 11.0
     */
    public SpringServlet(ApplicationContext context, boolean rootMapping) {
        this.context = context;
        this.rootMapping = rootMapping;
    }

    @Override
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        super.service(wrapRequest(request), response);
        closeResponseAfterForward(request, response);
    }

    /**
     * Closes the response at the end of a forward through the response the
     * servlet was given, as the container ends the response once the forward
     * returns anyway. Wrappers of filters earlier in the chain, such as the one
     * Spring Security writes its headers with, only see the response being
     * committed when it happens through them. Tomcat 11 by default
     * ({@code suspendWrappedResponseAfterForward}) ends a forwarded response on
     * the innermost response, past those wrappers, so their headers would be
     * dropped. Does nothing for a request that was not forwarded or that
     * started asynchronous processing.
     *
     * @param request
     *            the request the servlet handled
     * @param response
     *            the response the servlet was given
     */
    static void closeResponseAfterForward(HttpServletRequest request,
            HttpServletResponse response) {
        if (request.getDispatcherType() != DispatcherType.FORWARD
                || request.isAsyncStarted()) {
            return;
        }
        try {
            response.getOutputStream().close();
        } catch (IllegalStateException e) {
            getLogger().trace(
                    "The forwarded response was written through the writer", e);
            try {
                response.getWriter().close();
            } catch (IOException | IllegalStateException closeException) {
                logIgnoredCloseFailure(closeException);
            }
        } catch (IOException e) {
            logIgnoredCloseFailure(e);
        }
    }

    private static void logIgnoredCloseFailure(Exception e) {
        // The container ignores a failed close too, typically a client that
        // went away
        getLogger().debug("Unable to close the forwarded response", e);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(SpringServlet.class);
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
        Properties properties = config(initParameters);
        return super.createDeploymentConfiguration(properties);
    }

    private HttpServletRequest wrapRequest(HttpServletRequest request) {
        if (rootMapping && request.getPathInfo() == null) {
            /*
             * We need to apply a workaround in case of forwarding
             *
             * see https://jira.spring.io/browse/SPR-17457
             */
            return new ForwardingRequestWrapper(request);
        }
        return request;
    }

    private Properties config(Properties initParameters) {
        Properties properties = new Properties();
        properties.putAll(initParameters);
        PROPERTY_NAMES.forEach(property -> setProperty(property, properties));
        return properties;
    }

    private void setProperty(String property, Properties properties) {
        Environment env = context.getBean(Environment.class);
        String value = SpringUtil.getVaadinProperty(env, property);
        if (value != null) {
            properties.put(property, value);
        }
    }

}
