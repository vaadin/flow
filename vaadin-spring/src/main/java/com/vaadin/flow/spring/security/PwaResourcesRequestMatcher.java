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
package com.vaadin.flow.spring.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

/**
 * Matches requests for the PWA manifest, the offline page and the additional
 * offline resources configured by the application.
 *
 * <p>
 * The paths are computed by analyzing the {@link PWA} annotation on the
 * {@link AppShellConfigurator} implementor class. Flow puts them into the
 * service worker precache manifest, so the service worker requests them while
 * installing, and the installation fails if any of them is not served.
 *
 * <p>
 * The default manifest ({@link PwaConfiguration#DEFAULT_PATH}) and offline
 * ({@link PwaConfiguration#DEFAULT_OFFLINE_PATH}) paths are not considered,
 * since they are already part of {@link HandlerHelper#getPublicResources()}.
 */
public class PwaResourcesRequestMatcher implements RequestMatcher {

    private final RequestMatcher matcher;

    /**
     * Creates a new PwaResourcesRequestMatcher.
     *
     * @param service
     *            VaadinService instance, not {@literal null}.
     * @param urlMapping
     *            Vaadin servlet url mapping, can be {@literal null}.
     */
    public PwaResourcesRequestMatcher(VaadinService service,
            String urlMapping) {
        matcher = initMatchers(service, urlMapping);
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return matcher.matches(request);
    }

    private static RequestMatcher initMatchers(VaadinService service,
            String urlMapping) {
        UnaryOperator<String> urlMapper = path -> RequestUtil
                .applyUrlMapping(urlMapping, path);
        PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher
                .withDefaults();
        return RequestMatchers.anyOf(resourcePaths(service, urlMapper).stream()
                .map(path -> toMatcher(builder, path)).filter(Objects::nonNull)
                .toArray(RequestMatcher[]::new));
    }

    /**
     * Creates a matcher for a single configured path, or returns {@code null}
     * if the path cannot be turned into one.
     * <p>
     * A path is discarded on its own, so that an unusable one neither hides the
     * other configured resources nor makes this matcher throw on every request,
     * which would take down the whole application.
     */
    private static RequestMatcher toMatcher(
            PathPatternRequestMatcher.Builder builder, String path) {
        try {
            // The service worker only fetches these resources while
            // precaching, so permitting GET is enough
            return builder.matcher(HttpMethod.GET, path);
        } catch (RuntimeException ex) {
            LoggerFactory.getLogger(PwaResourcesRequestMatcher.class).error(
                    "Cannot create a request matcher for the PWA resource '{}'. "
                            + "Requests for it will be blocked by Spring Security.",
                    path, ex);
            return null;
        }
    }

    private static Set<String> resourcePaths(VaadinService service,
            UnaryOperator<String> urlMapper) {
        Set<String> paths = new LinkedHashSet<>();
        PwaConfiguration configuration = findPwaConfiguration(service);
        if (configuration == null) {
            return paths;
        }
        if (!PwaConfiguration.DEFAULT_PATH
                .equals(configuration.getManifestPath())) {
            paths.add(resolve(configuration.getManifestPath(), urlMapper));
        }
        if (configuration.isOfflinePathEnabled()
                && !PwaConfiguration.DEFAULT_OFFLINE_PATH
                        .equals(configuration.getOfflinePath())) {
            paths.add(resolve(configuration.getOfflinePath(), urlMapper));
        }
        configuration.getOfflineResources().stream()
                // Precache entries are written into a JavaScript literal, so
                // quotes are stripped from the URL the service worker requests
                .map(resource -> resource.replace("'", ""))
                .filter(resource -> !resource.isEmpty())
                .map(resource -> resolve(resource, urlMapper))
                .forEach(paths::add);
        return paths;
    }

    /**
     * Resolves a configured PWA resource into the path the service worker
     * requests it from.
     * <p>
     * Precache entries are resolved against the service worker location, so a
     * relative path lands under the Vaadin servlet mapping, while one starting
     * with a slash is requested from the origin root and is therefore matched
     * as it is. Note that an absolute path only reaches the application when it
     * is deployed on the root context.
     */
    private static String resolve(String path,
            UnaryOperator<String> urlMapper) {
        return path.startsWith("/") ? path : urlMapper.apply(path);
    }

    private static PwaConfiguration findPwaConfiguration(
            VaadinService service) {
        // Try first if there is an AppShell for the project, otherwise use the
        // class reported by the router
        Class<?> pwaAnnotatedClass = AppShellRegistry
                .getInstance(service.getContext()).getShell();
        if (pwaAnnotatedClass == null) {
            pwaAnnotatedClass = ApplicationRouteRegistry
                    .getInstance(service.getContext())
                    .getPwaConfigurationClass();
        }
        PWA pwa = pwaAnnotatedClass != null
                ? pwaAnnotatedClass.getAnnotation(PWA.class)
                : null;
        return pwa != null ? new PwaConfiguration(pwa) : null;
    }
}
