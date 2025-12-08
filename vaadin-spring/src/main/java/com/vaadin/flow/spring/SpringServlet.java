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
package com.vaadin.flow.spring;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.mvc.ServletForwardingController;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.shared.util.SharedUtil;

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

    private final ApplicationContext context;
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
     */
    public SpringServlet(ApplicationContext context, boolean rootMapping) {
        this.context = context;
        this.rootMapping = rootMapping;
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
        // transfer non-string init parameters (such as
        // DeploymentConfigurationFactory.FALLBACK_CHUNK)
        initParameters.forEach((key, value) -> {
            if (!(key instanceof String)) {
                properties.put(key, value);
            }
        });
        return properties;
    }

    private void setProperty(String property, Properties properties) {
        setProperty("vaadin." + property, property, properties);
    }

    private void setProperty(String envProperty, String initParam,
            Properties properties) {
        Environment env = context.getBean(Environment.class);
        String value = env.getProperty(upperCaseToDashSeparated(envProperty));
        if (value == null) {
            value = env.getProperty(envProperty);
        }
        if (value != null) {
            properties.put(initParam, value);
        }
    }

    private String upperCaseToDashSeparated(String value) {
        String result = value;
        if (result == null) {
            return null;
        }
        while (!result.equals(result.toLowerCase(Locale.ENGLISH))) {
            result = SharedUtil.camelCaseToDashSeparated(result);
        }
        return result;
    }

}
