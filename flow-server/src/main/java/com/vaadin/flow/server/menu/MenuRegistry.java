/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow.server.menu;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.MenuData;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.internal.ParameterInfo;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FrontendUtils;

/**
 * Registry for getting the menu items available for the current state of the
 * application.
 *
 * Only returns views that are accessible at the moment and leaves out routes
 * that require path parameters.
 */
public class MenuRegistry {

    /**
     * Collect views with menu annotation for automatic menu population. All
     * client views are collected and any accessible server views.
     *
     * @return routes with view information
     */
    public Map<String, ViewInfo> getMenuItems() {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forApplicationScope();

        Map<String, ViewInfo> menuRoutes = new HashMap<>();

        menuRoutes.putAll(collectClientMenuItems());

        List<RouteData> registeredAccessibleMenuRoutes = routeConfiguration
                .getRegisteredAccessibleMenuRoutes();

        for (RouteData route : registeredAccessibleMenuRoutes) {
            String title = getTitle(route);
            Map<String, RouteParamType> parameters = getParameters(route);
            menuRoutes.put("/" + route.getTemplate(),
                    new ViewInfo(title, null, false, "/" + route.getTemplate(),
                            false, false, route.getMenuData(), null,
                            parameters));
        }

        return menuRoutes;
    }

    /**
     * Get page title for route or simple name if no PageTitle is set.
     *
     * @param route
     *            route to get title for
     * @return title to use for route
     */
    private static String getTitle(RouteData route) {
        return Optional
                .ofNullable(route.getNavigationTarget().getClass()
                        .getAnnotation(PageTitle.class))
                .map(PageTitle::value)
                .orElse(route.getNavigationTarget().getSimpleName());
    }

    /**
     * Map route parameters to {@link RouteParamType}.
     *
     * @param route
     *            route to get params for
     * @return RouteParamType for parameter
     */
    private static Map<String, RouteParamType> getParameters(RouteData route) {
        Map<String, RouteParamType> parameters = new HashMap<>();

        route.getRouteParameters().forEach((paramTemplate, param) -> {
            ParameterInfo parameterInfo = new ParameterInfo(
                    param.getTemplate());
            parameters.put(param.getTemplate(),
                    RouteParamType.getType(parameterInfo));
        });
        return parameters;
    }

    private Map<String, ViewInfo> collectClientMenuItems() {
        List<String> clientRoutes = FrontendUtils.getClientRoutes();

        if (clientRoutes.isEmpty()) {
            // No client routes no need to do more work here.
            return Collections.emptyMap();
        }

        DeploymentConfiguration deploymentConfiguration = VaadinService
                .getCurrent().getDeploymentConfiguration();
        URL viewsJsonAsResource = getViewsJsonAsResource(
                deploymentConfiguration);
        if (viewsJsonAsResource == null) {
            LoggerFactory.getLogger(MenuRegistry.class).debug(
                    "No {} found under {} directory. Skipping client route registration.",
                    FILE_ROUTES_JSON_NAME,
                    deploymentConfiguration.isProductionMode()
                            ? "'META-INF/VAADIN'"
                            : "'frontend/generated'");
            return Collections.emptyMap();
        }

        Map<String, ViewInfo> configurations = new HashMap<>();

        try (InputStream source = viewsJsonAsResource.openStream()) {
            if (source != null) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.readValue(source, new TypeReference<List<ViewInfo>>() {
                }).forEach(clientViewConfig -> collectClientViews("",
                        clientViewConfig, configurations));
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(MenuRegistry.class).warn(
                    "Failed load {} from {}", FILE_ROUTES_JSON_NAME,
                    viewsJsonAsResource.getPath(), e);
        }

        for (String route : new HashSet<>(configurations.keySet())) {
            if (!clientRoutes.contains(route)) {
                configurations.remove(route);
            }
        }

        return configurations;
    }

    private void collectClientViews(String basePath, ViewInfo viewConfig,
            Map<String, ViewInfo> configurations) {
        String path = viewConfig.route() == null || viewConfig.route().isEmpty()
                ? basePath
                : basePath.isEmpty() ? viewConfig.route()
                        : basePath + '/' + viewConfig.route();
        configurations.put(path, viewConfig);
        if (viewConfig.children() != null) {
            viewConfig.children().forEach(
                    child -> collectClientViews(path, child, configurations));
        }
    }

    public static final String FILE_ROUTES_JSON_NAME = "file-routes.json";
    public static final String FILE_ROUTES_JSON_PROD_PATH = "/META-INF/VAADIN/"
            + FILE_ROUTES_JSON_NAME;

    private URL getViewsJsonAsResource(
            DeploymentConfiguration deploymentConfiguration) {
        var isProductionMode = deploymentConfiguration.isProductionMode();
        if (isProductionMode) {
            return getClass().getResource(FILE_ROUTES_JSON_PROD_PATH);
        }
        try {
            Path fileRoutes = deploymentConfiguration.getFrontendFolder()
                    .toPath().resolve("generated")
                    .resolve(FILE_ROUTES_JSON_NAME);
            if (fileRoutes.toFile().exists()) {
                return fileRoutes.toUri().toURL();
            }
            return null;
        } catch (MalformedURLException e) {
            LoggerFactory.getLogger(MenuRegistry.class).warn(
                    "Failed to find {} under frontend/generated",
                    FILE_ROUTES_JSON_NAME, e);
            throw new RuntimeException(e);
        }
    }

}
