/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.spring.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * Matches request for custom PWA icons and Favicon paths.
 *
 * PWA icon paths are computed by analyzing the {@link PWA} annotation on the
 * {@link AppShellConfigurator} implementor class. The favicon is detected by
 * invoking the {@link AppShellConfigurator#configurePage(AppShellSettings)}
 * method and tracking potential calls to
 * {@link AppShellSettings#addFavIcon(String, String, String)} and
 * {@link AppShellSettings#addFavIcon(String, String, String)} methods.
 *
 * Default paths ({@link PwaConfiguration#DEFAULT_ICON} and
 * {@literal /favicon.ico}) are not considered.
 */
public class WebIconsRequestMatcher implements RequestMatcher {

    private final RequestMatcher matcher;

    /**
     * Creates a new WebIconsRequestMatcher.
     *
     * @param service
     *            VaadinService instance, not {@literal null}.
     * @param urlMapping
     *            Vaadin servlet url mapping, can be {@literal null}.
     */
    public WebIconsRequestMatcher(VaadinService service, String urlMapping) {
        matcher = initMatchers(service, urlMapping);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return matcher.matches(request);
    }

    private static RequestMatcher initMatchers(VaadinService service,
            String urlMapping) {

        AppShellRegistry appShellRegistry = AppShellRegistry
                .getInstance(service.getContext());
        Class<? extends AppShellConfigurator> appShellClass = appShellRegistry
                .getShell();

        UnaryOperator<String> urlMapper = path -> RequestUtil
                .applyUrlMapping(urlMapping, path);
        Set<String> paths = new HashSet<>();
        appendFavIconPath(paths, appShellClass, service, urlMapper);
        appendPwaIconPaths(paths, appShellClass, service, urlMapper);
        return RequestMatchers
                .anyOf(RequestUtil.antMatchers(paths.toArray(String[]::new)));
    }

    private static void appendFavIconPath(Set<String> paths,
            Class<? extends AppShellConfigurator> appShellClass,
            VaadinService vaadinService, UnaryOperator<String> urlMapper) {
        if (appShellClass != null) {

            AppShellSettings settings = new AppShellSettings() {
                @Override
                public void addFavIcon(String rel, String href, String sizes) {
                    registerPath(href);
                }

                @Override
                public void addFavIcon(Inline.Position position, String rel,
                        String href, String sizes) {
                    registerPath(href);
                }

                private void registerPath(String path) {
                    if (!path.startsWith("/")) {
                        path = urlMapper.apply(path);
                    }
                    paths.add(path);
                }
            };
            try {
                vaadinService.getInstantiator().getOrCreate(appShellClass)
                        .configurePage(settings);
            } catch (Exception ex) {
                LoggerFactory.getLogger(WebIconsRequestMatcher.class)
                        .debug("Cannot detect favicon path", ex);
            }
        }
        // Remove default favicon paths
        paths.remove("/favicon.ico");
    }

    private static void appendPwaIconPaths(Set<String> paths,
            Class<?> appShellClass, VaadinService vaadinService,
            UnaryOperator<String> urlMapper) {
        Class<?> pwaAnnotatedClass = appShellClass;
        // Otherwise use the class reported by router
        if (pwaAnnotatedClass == null) {
            pwaAnnotatedClass = ApplicationRouteRegistry
                    .getInstance(vaadinService.getContext())
                    .getPwaConfigurationClass();
        }
        // Initialize PwaRegistry with found PWA settings
        PWA pwa = pwaAnnotatedClass != null
                ? pwaAnnotatedClass.getAnnotation(PWA.class)
                : null;
        if (pwa != null
                && !PwaConfiguration.DEFAULT_ICON.equals(pwa.iconPath())) {

            // Base icon is not served by PwaHandler, so it is not aware of
            // urlMapping
            String baseIconPrefix = pwa.iconPath().startsWith("/") ? "" : "/";
            paths.add(baseIconPrefix + pwa.iconPath());

            HandlerHelper.getIconVariants(pwa.iconPath()).stream()
                    .map(urlMapper)
                    .collect(Collectors.toCollection(() -> paths));
        }

    }
}
