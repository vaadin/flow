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

package com.vaadin.flow.internal.menu;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.MenuData;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteParameterData;
import com.vaadin.flow.router.internal.ParameterInfo;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.menu.AvailableViewInfo;
import com.vaadin.flow.server.menu.RouteParamType;

import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;

/**
 * Registry for getting the menu items available for the current state of the
 * application.
 *
 * Only returns views that are accessible at the moment and leaves out routes
 * that require path parameters.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class MenuRegistry {

    private static final Logger log = LoggerFactory
            .getLogger(MenuRegistry.class);

    /**
     * Collect views with menu annotation for automatic menu population. All
     * client views are collected and any accessible server views.
     *
     * @return routes with view information
     */
    public static Map<String, AvailableViewInfo> collectMenuItems() {
        Map<String, AvailableViewInfo> menuRoutes = MenuRegistry
                .getMenuItems(true);
        menuRoutes.entrySet()
                .removeIf(entry -> Optional.ofNullable(entry.getValue())
                        .map(AvailableViewInfo::menu).map(MenuData::isExclude)
                        .orElse(false));
        return menuRoutes;
    }

    /**
     * Collect ordered list of views with menu annotation for automatic menu
     * population. All client views are collected and any accessible server
     * views.
     *
     * @return ordered routes with view information
     */
    public static List<AvailableViewInfo> collectMenuItemsList() {
        // en-US is used by default here to match with Hilla's
        // createMenuItems.ts sorting algorithm.
        return collectMenuItemsList(Locale.forLanguageTag("en-US"));
    }

    /**
     * Collect ordered list of views with menu annotation for automatic menu
     * population. All client views are collected and any accessible server
     * views.
     *
     * @param locale
     *            locale to use for ordering. null for default locale.
     * @return ordered routes with view information
     */
    public static List<AvailableViewInfo> collectMenuItemsList(Locale locale) {
        return collectMenuItems().entrySet().stream().map(entry -> {
            AvailableViewInfo value = entry.getValue();
            return new AvailableViewInfo(value.title(), value.rolesAllowed(),
                    value.loginRequired(), entry.getKey(), value.lazy(),
                    value.register(), value.menu(), value.children(),
                    value.routeParameters(), value.flowLayout());
        }).sorted(getMenuOrderComparator(
                (locale != null ? Collator.getInstance(locale)
                        : Collator.getInstance())))
                .toList();
    }

    /**
     * Collect views with menu annotation for automatic menu population. All
     * client views are collected and any accessible server views.
     *
     * @param filterClientViews
     *            {@code true} to filter routes by authentication status
     * @return routes with view information
     */
    public static Map<String, AvailableViewInfo> getMenuItems(
            boolean filterClientViews) {
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forApplicationScope();

        Map<String, AvailableViewInfo> menuRoutes = new HashMap<>(
                collectClientMenuItems(filterClientViews, VaadinService
                        .getCurrent().getDeploymentConfiguration()));

        collectAndAddServerMenuItems(routeConfiguration, menuRoutes);

        return menuRoutes;
    }

    /**
     * Collect all active and accessible server menu items.
     *
     * @param routeConfiguration
     *            routeConfiguration to use
     * @param menuRoutes
     *            map to add route data into
     */
    public static void collectAndAddServerMenuItems(
            RouteConfiguration routeConfiguration,
            Map<String, AvailableViewInfo> menuRoutes) {
        List<RouteData> registeredAccessibleMenuRoutes = routeConfiguration
                .getRegisteredAccessibleMenuRoutes();

        addMenuRoutes(menuRoutes, registeredAccessibleMenuRoutes);
    }

    /**
     * Collect all active and accessible server menu items.
     *
     * @param routeConfiguration
     *            routeConfiguration to use
     * @param accessControls
     *            extra access controls if needed
     * @param menuRoutes
     *            map to add route data into
     */
    public static void collectAndAddServerMenuItems(
            RouteConfiguration routeConfiguration,
            List<BeforeEnterListener> accessControls,
            Map<String, AvailableViewInfo> menuRoutes) {
        List<RouteData> registeredAccessibleMenuRoutes = routeConfiguration
                .getRegisteredAccessibleMenuRoutes(accessControls);

        addMenuRoutes(menuRoutes, registeredAccessibleMenuRoutes);
    }

    private static void addMenuRoutes(Map<String, AvailableViewInfo> menuRoutes,
            List<RouteData> registeredAccessibleMenuRoutes) {
        for (RouteData route : registeredAccessibleMenuRoutes) {
            String title = getTitle(route.getNavigationTarget());
            final String url = getRouteUrl(route);
            Map<String, RouteParamType> parameters = getParameters(route);
            menuRoutes.put(url,
                    new AvailableViewInfo(title, null, false, url, false, false,
                            route.getMenuData(), null, parameters, false));
        }
    }

    /**
     * Get the route url for the route. If the route has optional parameters,
     * the url is stripped off from them.
     *
     * @param route
     *            route to get url for
     * @return url for the route
     */
    private static String getRouteUrl(RouteData route) {
        if (route.getRouteParameters() != null
                && !route.getRouteParameters().isEmpty()) {
            String editUrl = "/" + route.getTemplate();
            for (RouteParameterData param : route.getRouteParametersList()
                    .stream()
                    .filter(param -> param.isOptional() || param.isVarargs())
                    .toList()) {
                editUrl = editUrl.replace("/" + param.getTemplate(), "");
            }
            if (editUrl.isEmpty()) {
                editUrl = "/";
            }
            return editUrl;
        } else {
            return "/" + route.getTemplate();
        }
    }

    /**
     * Get page title for route or simple name if no PageTitle is set.
     *
     * @param target
     *            route class to get title for
     * @return title to use for route
     */
    public static String getTitle(Class<? extends Component> target) {
        return Optional.ofNullable(target.getAnnotation(PageTitle.class))
                .map(PageTitle::value).orElse(target.getSimpleName());
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

    /**
     * Collect all available client routes.
     *
     * @param filterClientViews
     *            {@code true} to filter routes by authentication status
     * @param configuration
     *            application configuration
     * @return map of registered routes
     */
    public static Map<String, AvailableViewInfo> collectClientMenuItems(
            boolean filterClientViews, AbstractConfiguration configuration) {

        VaadinRequest vaadinRequest = VaadinRequest.getCurrent();
        return collectClientMenuItems(filterClientViews, configuration,
                vaadinRequest);
    }

    /**
     * Get registered client routes. Possible to have all routes or only
     * accessible routes.
     *
     * @param filterClientViews
     *            {@code true} to filter routes by authentication status
     * @param configuration
     *            application configuration
     * @return list of available client routes
     */
    public static List<String> getClientRoutes(boolean filterClientViews,
            AbstractConfiguration configuration) {

        VaadinRequest vaadinRequest = VaadinRequest.getCurrent();
        return new ArrayList<>(collectClientMenuItems(filterClientViews,
                configuration, vaadinRequest).keySet());
    }

    /**
     * Collect all available client routes.
     *
     * @param filterClientViews
     *            {@code true} to filter routes by authentication status
     * @param configuration
     *            application configuration
     * @param vaadinRequest
     *            current request
     * @return map of registered routes
     */
    public static Map<String, AvailableViewInfo> collectClientMenuItems(
            boolean filterClientViews, AbstractConfiguration configuration,
            VaadinRequest vaadinRequest) {

        URL viewsJsonAsResource = getViewsJsonAsResource(configuration);
        if (viewsJsonAsResource == null) {
            LoggerFactory.getLogger(MenuRegistry.class).debug(
                    "No {} found under {} directory. Skipping client route registration.",
                    FILE_ROUTES_JSON_NAME,
                    configuration.isProductionMode() ? "'META-INF/VAADIN'"
                            : "'frontend/generated'");
            return Collections.emptyMap();
        }

        Map<String, AvailableViewInfo> configurations = new HashMap<>();

        try (InputStream source = viewsJsonAsResource.openStream()) {
            if (source != null) {
                ObjectMapper mapper = new ObjectMapper().configure(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false);
                mapper.readValue(source,
                        new TypeReference<List<AvailableViewInfo>>() {
                        }).forEach(clientViewConfig -> collectClientViews("",
                                clientViewConfig, configurations));
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(MenuRegistry.class).warn(
                    "Failed load {} from {}", FILE_ROUTES_JSON_NAME,
                    viewsJsonAsResource.getPath(), e);
        }

        if (filterClientViews) {
            filterClientViews(configurations, vaadinRequest);
        }

        return configurations;
    }

    private static void collectClientViews(String basePath,
            AvailableViewInfo viewConfig,
            Map<String, AvailableViewInfo> configurations) {
        String path = viewConfig.route() == null || viewConfig.route().isEmpty()
                ? basePath
                : viewConfig.route().startsWith("/")
                        ? basePath + viewConfig.route()
                        : basePath + '/' + viewConfig.route();
        if (viewConfig.menu() == null) {
            // create MenuData anyway to avoid need for null checking
            viewConfig = copyAvailableViewInfo(viewConfig,
                    new MenuData(viewConfig.title(), null, false, null, null));
        }
        configurations.put(path, viewConfig);
        if (viewConfig.children() != null) {
            viewConfig.children().forEach(
                    child -> collectClientViews(path, child, configurations));
        }
    }

    private static AvailableViewInfo copyAvailableViewInfo(
            AvailableViewInfo source, MenuData newMenuData) {
        return new AvailableViewInfo(source.title(), source.rolesAllowed(),
                source.loginRequired(), source.route(), source.lazy(),
                source.register(), newMenuData, source.children(),
                source.routeParameters(), source.flowLayout());
    }

    public static final String FILE_ROUTES_JSON_NAME = "file-routes.json";
    public static final String FILE_ROUTES_JSON_PROD_PATH = "META-INF/VAADIN/"
            + FILE_ROUTES_JSON_NAME;

    /**
     * Load views json as a resource.
     *
     * @param configuration
     *            current application configuration
     * @return URL to json resource
     */
    public static URL getViewsJsonAsResource(
            AbstractConfiguration configuration) {
        var isProductionMode = configuration.isProductionMode();
        if (isProductionMode) {
            return getClassLoader().getResource(FILE_ROUTES_JSON_PROD_PATH);
        }
        try {
            Path fileRoutes = configuration.getFrontendFolder().toPath()
                    .resolve(GENERATED).resolve(FILE_ROUTES_JSON_NAME);
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

    private static void filterClientViews(
            Map<String, AvailableViewInfo> configurations,
            VaadinRequest vaadinRequest) {
        final boolean isUserAuthenticated = vaadinRequest
                .getUserPrincipal() != null;

        Set<String> clientEntries = new HashSet<>(configurations.keySet());
        for (String key : clientEntries) {
            if (!configurations.containsKey(key)) {
                // view may have been removed together with parent
                continue;
            }
            AvailableViewInfo viewInfo = configurations.get(key);
            boolean routeValid = validateViewAccessible(viewInfo,
                    isUserAuthenticated, vaadinRequest::isUserInRole);

            if (!routeValid) {
                configurations.remove(key);
                if (viewInfo.children() != null
                        && !viewInfo.children().isEmpty()) {
                    // remove all children for unauthenticated parent.
                    removeChildren(configurations, viewInfo, key);
                }
            }
        }
    }

    private static void removeChildren(
            Map<String, AvailableViewInfo> configurations,
            AvailableViewInfo viewInfo, String parentPath) {
        for (AvailableViewInfo child : viewInfo.children()) {
            configurations.remove(parentPath + "/" + child.route());
            if (child.children() != null) {
                removeChildren(configurations, child,
                        parentPath + "/" + child.route());
            }
        }
    }

    /**
     * Check view against authentication state.
     * <p>
     * If not authenticated and login required -> invalid. If user doesn't have
     * correct roles -> invalid.
     *
     * @param viewInfo
     *            view info
     * @param isUserAuthenticated
     *            user authentication state
     * @param roleAuthentication
     *            method to authenticate if user has role
     * @return true if accessible, false if something is not authenticated
     */
    private static boolean validateViewAccessible(AvailableViewInfo viewInfo,
            boolean isUserAuthenticated,
            Predicate<? super String> roleAuthentication) {
        if (viewInfo.loginRequired() && !isUserAuthenticated) {
            return false;
        }
        String[] roles = viewInfo.rolesAllowed();
        return roles == null || roles.length == 0
                || Arrays.stream(roles).anyMatch(roleAuthentication);
    }

    /**
     * Get the current thread ContextClassLoader.
     * <p>
     * Note! public for testing.
     *
     * @return ClassLoader
     */
    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * See if there is a client route available for given route path.
     *
     * @param route
     *            route path to check
     * @return true if a client route is found.
     */
    public static boolean hasClientRoute(String route) {
        return hasClientRoute(route, false);
    }

    /**
     * See if there is a client route available for given route path, optionally
     * excluding layouts (routes with children) from the check.
     *
     * @param route
     *            route path to check
     * @param excludeLayouts
     *            {@literal true} to exclude layouts from the check,
     *            {@literal false} to include them
     * @return true if a client route is found.
     */
    public static boolean hasClientRoute(String route, boolean excludeLayouts) {
        if (route == null) {
            return false;
        }
        route = route.isEmpty() ? route
                : route.startsWith("/") ? route : "/" + route;
        return getClientRoutes(excludeLayouts).containsKey(route);
    }

    /**
     * Get available client routes, optionally excluding any layout targets.
     *
     * @param excludeLayouts
     *            {@literal true} to exclude layouts from the check,
     *            {@literal false} to include them
     * @return Map of client routes available
     */
    public static Map<String, AvailableViewInfo> getClientRoutes(
            boolean excludeLayouts) {
        if (VaadinSession.getCurrent() == null) {
            return Collections.emptyMap();
        }
        Map<String, AvailableViewInfo> clientItems = MenuRegistry
                .collectClientMenuItems(true,
                        VaadinSession.getCurrent().getConfiguration());
        if (excludeLayouts) {
            clientItems = clientItems.entrySet().stream()
                    .filter(entry -> entry.getValue().children() == null)
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue));
        }
        return clientItems;
    }

    private static Comparator<AvailableViewInfo> getMenuOrderComparator(
            Collator collator) {
        return (o1, o2) -> {
            int ordersCompareTo = Optional.ofNullable(o1.menu())
                    .map(MenuData::getOrder).orElse(Double.MAX_VALUE)
                    .compareTo(Optional.ofNullable(o2.menu())
                            .map(MenuData::getOrder).orElse(Double.MAX_VALUE));
            return ordersCompareTo != 0 ? ordersCompareTo
                    : collator.compare(o1.route(), o2.route());
        };
    }
}
