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

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.ServletForwardingController;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;

import com.vaadin.flow.server.VaadinServlet;

/**
 * Vaadin servlet configuration.
 * <p>
 * The configuration is used only when the Vaadin servlet is mapped to the root
 * ({@literal "/*"}) because in this case the servlet is mapped to
 * {@literal "/vaadinServlet/*"} instead of ({@literal "/*"}). It's done to make
 * possible to configure other Spring services (like endpoints) which have
 * overlapping path.
 *
 *
 * @author Vaadin Ltd
 *
 */
@Configuration
@Conditional(RootMappedCondition.class)
public class VaadinServletConfiguration {

    static final String VAADIN_SERVLET_MAPPING = "/vaadinServlet/*";
    public static final String EXCLUDED_URLS_PROPERTY = "vaadin.excludeUrls";

    /**
     * Gets the excluded URLs in a way compatible with both plain Spring and
     * Spring Boot.
     *
     * @param environment
     *            the application environment
     * @return the excluded URLs or null if none is defined
     */
    private static List<String> getExcludedUrls(Environment environment) {
        if (SpringUtil.isSpringBoot()) {
            try {
                return (List<String>) Class.forName(
                        "com.vaadin.flow.spring.VaadinConfigurationProperties")
                        .getMethod("getExcludedUrls", Environment.class)
                        .invoke(null, environment);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException
                    | SecurityException | ClassNotFoundException e) {
                LoggerFactory.getLogger(RootMappedCondition.class).error(
                        "Unable to find excluded URLs from properties", e);
                return null;
            }
        } else {
            String value = environment.getProperty(EXCLUDED_URLS_PROPERTY);
            if (value == null || value.isEmpty()) {
                return Collections.emptyList();
            } else {
                return Arrays.stream(value.split(",")).map(url -> url.trim())
                        .collect(Collectors.toList());
            }
        }
    }

    public static class RootExcludeHandler extends SimpleUrlHandlerMapping {
        private List<String> excludeUrls;
        private AntPathMatcher matcher;
        private UrlPathHelper urlPathHelper = new UrlPathHelper();
        private HandlerMapping resourceHandlerMapping;

        public RootExcludeHandler(List<String> excludeUrls,
                Controller vaadinForwardingController,
                HandlerMapping resourceHandlerMapping) {
            this.excludeUrls = excludeUrls;
            this.resourceHandlerMapping = resourceHandlerMapping;
            matcher = new AntPathMatcher();

            setOrder(Ordered.LOWEST_PRECEDENCE - 1);

            // This is /** and not /* so that it is not interpreted as a
            // "default handler"
            // and we can override the behavior in getHandlerInternal
            setUrlMap(Collections.singletonMap("/**",
                    vaadinForwardingController));
        }

        @Override
        protected Object getHandlerInternal(HttpServletRequest request)
                throws Exception {
            if (excludeUrls != null && !excludeUrls.isEmpty()) {
                String requestPath = urlPathHelper
                        .getPathWithinApplication(request);
                for (String pattern : excludeUrls) {
                    if (matcher.match(pattern, requestPath)) {
                        getLogger().debug(
                                "Ignoring request to {} excluded by {}",
                                requestPath, pattern);
                        return null;
                    }
                }
            }
            if (resourceHandlerMapping != null) {
                // Check if the request is for a static resource
                HandlerExecutionChain handler = resourceHandlerMapping
                        .getHandler(request);
                if (handler != null) {
                    Object innerHandler = handler.getHandler();
                    if (innerHandler instanceof ResourceHttpRequestHandler resourceHttpRequestHandler) {
                        if (!mapsToRoot(resourceHttpRequestHandler)) {
                            // We cannot use the context resource mapped to / as
                            // it would overwrite all
                            // routes. Anything mapped to only a certain part of
                            // the app is okay to use.
                            return handler;
                        }
                    }
                }
            }
            return super.getHandlerInternal(request);
        }

        private boolean mapsToRoot(
                ResourceHttpRequestHandler resourceHttpRequestHandler) {
            return resourceHttpRequestHandler.getLocations().stream()
                    .anyMatch(location -> {
                        return location instanceof ServletContextResource servletContextResource
                                && "/".equals(servletContextResource.getPath());
                    });
        }

        protected Logger getLogger() {
            return LoggerFactory.getLogger(getClass());
        }

    }

    /**
     * Makes an url handler mapping allowing to forward requests from a
     * {@link DispatcherServlet} to {@link VaadinServlet}.
     *
     * @return an url handler mapping instance which forwards requests to vaadin
     *         servlet
     */
    @Bean
    public RootExcludeHandler vaadinRootMapping(Environment environment,
            @Autowired(required = false) @Qualifier("resourceHandlerMapping") HandlerMapping resourceHandlerMapping) {
        return new RootExcludeHandler(getExcludedUrls(environment),
                vaadinForwardingController(), resourceHandlerMapping);
    }

    /**
     * Makes a forwarding controller.
     *
     * @return a forwarding controller
     */
    @Bean
    public Controller vaadinForwardingController() {
        ServletForwardingController controller = new ServletForwardingController();
        controller.setServletName(
                ClassUtils.getShortNameAsProperty(SpringServlet.class));
        return controller;
    }

}
