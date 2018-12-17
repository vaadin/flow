/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.router.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;

/**
 * Utility class with methods for route handling.
 */
public final class RouteUtil {

    private RouteUtil() {
    }

    /**
     * Get parent layouts for navigation target {@link Route} annotation.
     *
     * @param component
     *         navigation target to get parents for
     * @return parent layouts for target
     */
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            Class<?> component) {
        Optional<Route> route = AnnotationReader
                .getAnnotationFor(component, Route.class);
        if (route.isPresent() && !route.get().layout().equals(UI.class)) {
            return collectRouteParentLayouts(route.get().layout());
        }
        return Collections.emptyList();
    }

    /**
     * Get parent layouts for navigation target according to the {@link Route}
     * or {@link RouteAlias} annotation.
     *
     * @param component
     *         navigation target to get parents for
     * @param path
     *         path used to get navigation target so we know which annotation
     *         to handle
     * @return parent layouts for target
     */
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            Class<?> component, String path) {
        final List<Class<? extends RouterLayout>> list = new ArrayList<>();

        Optional<Route> route = AnnotationReader
                .getAnnotationFor(component, Route.class);
        List<RouteAlias> routeAliases = AnnotationReader
                .getAnnotationsFor(component, RouteAlias.class);
        if (route.isPresent() && path
                .equals(getRoutePath(component, route.get())) && !route.get()
                .layout().equals(UI.class)) {
            list.addAll(collectRouteParentLayouts(route.get().layout()));
        } else {

            Optional<RouteAlias> matchingRoute = getMatchingRouteAlias(
                    component, path, routeAliases);
            if (matchingRoute.isPresent()) {
                list.addAll(collectRouteParentLayouts(
                        matchingRoute.get().layout()));
            }
        }

        return list;
    }

    /**
     * Get the actual route path including all parent layout
     * {@link RoutePrefix}.
     *
     * @param component
     *         navigation target component to get route path for
     * @param route
     *         route annotation to check
     * @return actual path for given route target
     */
    public static String getRoutePath(Class<?> component, Route route) {
        if (route.absolute()) {
            return Router.resolve(component, route);
        }
        List<String> parentRoutePrefixes = getRoutePrefixes(component,
                route.layout(), Router.resolve(component, route));
        return parentRoutePrefixes.stream().collect(Collectors.joining("/"));
    }

    /**
     * Get the actual route path including all parent layout
     * {@link RoutePrefix}.
     *
     * @param component
     *         navigation target component to get route path for
     * @param alias
     *         route alias annotation to check
     * @return actual path for given route alias target
     */
    public static String getRouteAliasPath(Class<?> component,
            RouteAlias alias) {
        if (alias.absolute()) {
            return alias.value();
        }
        List<String> parentRoutePrefixes = getRoutePrefixes(component,
                alias.layout(), alias.value());
        return parentRoutePrefixes.stream().collect(Collectors.joining("/"));
    }

    private static List<String> getRoutePrefixes(Class<?> component,
            final Class<? extends RouterLayout> layout, final String value) {
        List<String> parentRoutePrefixes = getParentRoutePrefixes(component,
                () -> layout);
        Collections.reverse(parentRoutePrefixes);
        if (value != null && !value.isEmpty()) {
            parentRoutePrefixes.add(value);
        }

        return parentRoutePrefixes;
    }

    private static List<String> getParentRoutePrefixes(Class<?> component,
            Supplier<Class<? extends RouterLayout>> routerLayoutSupplier) {
        List<String> list = new ArrayList<>();

        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(component, ParentLayout.class);
        Optional<RoutePrefix> routePrefix = AnnotationReader
                .getAnnotationFor(component, RoutePrefix.class);

        routePrefix.ifPresent(prefix -> list.add(prefix.value()));

        // break chain on an absolute RoutePrefix or Route
        if (routePrefix.isPresent() && routePrefix.get().absolute()) {
            return list;
        }

        Class<? extends RouterLayout> routerLayout = routerLayoutSupplier.get();
        if (routerLayout != null && !routerLayout.equals(UI.class)) {
            list.addAll(getParentRoutePrefixes(routerLayout, () -> null));
        } else if (parentLayout.isPresent()) {
            list.addAll(getParentRoutePrefixes(parentLayout.get().value(),
                    () -> null));
        }

        return list;
    }

    private static Optional<RouteAlias> getMatchingRouteAlias(
            Class<?> component, String path, List<RouteAlias> routeAliases) {
        return routeAliases.stream().filter(alias ->
                path.equals(getRouteAliasPath(component, alias)) && !alias
                        .layout().equals(UI.class)).findFirst();
    }

    private static List<Class<? extends RouterLayout>> collectRouteParentLayouts(
            Class<? extends RouterLayout> layout) {
        List<Class<? extends RouterLayout>> layouts = new ArrayList<>();
        layouts.add(layout);

        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(layout, ParentLayout.class);
        if (parentLayout.isPresent()) {
            layouts.addAll(
                    collectRouteParentLayouts(parentLayout.get().value()));
        }
        return layouts;
    }

    /**
     * Collect possible route parent layouts for a navigation target that is not
     * annotated with {@link Route} nor {@link RouteAlias}, but may still
     * contain {@link ParentLayout}. Mainly error navigation targets.
     *
     * @param navigationTarget
     *         route to check parent layouts for
     * @return list of parent layouts
     */
    public static List<Class<? extends RouterLayout>> getParentLayoutsForNonRouteTarget(
            Class<?> navigationTarget) {
        List<Class<? extends RouterLayout>> layouts = new ArrayList<>();

        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(navigationTarget, ParentLayout.class);
        if (parentLayout.isPresent()) {
            layouts.addAll(
                    collectRouteParentLayouts(parentLayout.get().value()));
        }
        return layouts;
    }

    /**
     * Get the top most parent layout for navigation target according to the
     * {@link Route} or {@link RouteAlias} annotation. Also handles non route
     * targets with {@link ParentLayout}.
     *
     * @param component
     *         navigation target to get top most parent for
     * @param path
     *         path used to get navigation target so we know which annotation
     *         to handle or null for error views.
     * @return top parent layout for target or null if none found
     */
    public static Class<? extends RouterLayout> getTopParentLayout(
            final Class<?> component, final String path) {
        if (path == null) {
            Optional<ParentLayout> parentLayout = AnnotationReader
                    .getAnnotationFor(component, ParentLayout.class);
            if (parentLayout.isPresent()) {
                return recuseToTopLayout(parentLayout.get().value());
            }
            // No need to check for Route or RouteAlias as the path is null
            return null;
        }

        Optional<Route> route = AnnotationReader
                .getAnnotationFor(component, Route.class);
        List<RouteAlias> routeAliases = AnnotationReader
                .getAnnotationsFor(component, RouteAlias.class);
        if (route.isPresent() && path
                .equals(getRoutePath(component, route.get())) && !route.get()
                .layout().equals(UI.class)) {
            return recuseToTopLayout(route.get().layout());
        } else {
            Optional<RouteAlias> matchingRoute = getMatchingRouteAlias(
                    component, path, routeAliases);
            if (matchingRoute.isPresent()) {
                return recuseToTopLayout(matchingRoute.get().layout());
            }
        }

        return null;
    }

    private static Class<? extends RouterLayout> recuseToTopLayout(
            Class<? extends RouterLayout> layout) {
        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(layout, ParentLayout.class);

        if (parentLayout.isPresent()) {
            return recuseToTopLayout(parentLayout.get().value());
        }
        return layout;
    }

    /**
     * Updates the page title according to the currently visible component.
     * <p>
     * Uses {@link HasDynamicTitle#getPageTitle()} if implemented, or else the
     * {@link PageTitle} annotation, to resolve the title.
     *
     * @param navigationEvent
     *         the event object about the navigation
     * @param routeTarget
     *         the currently visible component
     */
    public static void updatePageTitle(NavigationEvent navigationEvent,
            Component routeTarget) {

        String title;

        if (routeTarget instanceof HasDynamicTitle) {
            title = ((HasDynamicTitle) routeTarget).getPageTitle();
        } else {
            title = lookForTitleInTarget(routeTarget).map(PageTitle::value)
                    .orElse("");
        }
        navigationEvent.getUI().getPage().setTitle(title);
    }

    private static Optional<PageTitle> lookForTitleInTarget(
            Component routeTarget) {
        return Optional.ofNullable(
                routeTarget.getClass().getAnnotation(PageTitle.class));
    }

    /**
     * Checks that the same component type is not used in multiple parts of a
     * route chain.
     *
     * @param routeTargetType
     *         the actual component in the route chain
     * @param routeLayoutTypes
     *         the parent types in the route chain
     */
    public static void checkForDuplicates(
            Class<? extends Component> routeTargetType,
            Collection<Class<? extends RouterLayout>> routeLayoutTypes) {
        Set<Class<?>> duplicateCheck = new HashSet<>();
        duplicateCheck.add(routeTargetType);
        for (Class<?> parentType : routeLayoutTypes) {
            if (!duplicateCheck.add(parentType)) {
                throw new IllegalArgumentException(
                        parentType + " is used in multiple locations");
            }
        }
    }

    /**
     * Create a new location change event for given navigation event and chain.
     *
     * @param event
     *         current navigation event
     * @param routeTargetChain
     *         chain of route targets
     * @return new LocationChangeEvent
     */
    public static LocationChangeEvent createEvent(NavigationEvent event,
            List<HasElement> routeTargetChain) {
        return new LocationChangeEvent(event.getSource(), event.getUI(),
                event.getTrigger(), event.getLocation(), routeTargetChain);
    }

    /**
     * Collect the whole route for the navigation target.
     * <p>
     * The whole route is composed of the Route annotation and any
     * ParentLayout:@RoutePrefix that may be in the navigation chain.
     *
     * @param navigationTarget
     *         navigation target to get chain route for
     * @return full navigation route
     */
    public static String getNavigationRoute(Class<?> navigationTarget) {
        Route annotation = navigationTarget.getAnnotation(Route.class);
        return RouteUtil.getRoutePath(navigationTarget, annotation);
    }

    /**
     * Collect route aliases for given navigation target.
     * <p>
     * The whole route is composed of the RouteAlias annotation and any
     * ParentLayout:@RoutePrefix that may be in the navigation chain.
     *
     * @param navigationTarget
     *         navigation target to get chain route alias for
     * @return full navigation route
     */
    public static Collection<String> getRouteAliases(
            Class<?> navigationTarget) {
        List<String> aliases = new ArrayList<>();
        for (RouteAlias alias : navigationTarget
                .getAnnotationsByType(RouteAlias.class)) {
            aliases.add(RouteUtil.getRouteAliasPath(navigationTarget, alias));
        }
        return aliases;
    }

    /**
     * Registers a set of components as navigation targets.
     * <p>
     * Given navigation targets need to have the {@link Route} annotation an
     * be {@link Component}s.
     *
     * @param navigationTargets
     *         set of navigation target components
     * @param registry
     *         RouteRegistry to add route to
     * @throws InvalidRouteConfigurationException
     *         if routing has been configured incorrectly
     */
    public static void setNavigationTargets(
            Set<Class<? extends Component>> navigationTargets,
            RouteRegistry registry) {
        List<Class<? extends Component>> faulty = navigationTargets.stream()
                .filter(target -> !target.isAnnotationPresent(Route.class))
                .filter(Component.class::isAssignableFrom)
                .collect(Collectors.toList());
        if (!faulty.isEmpty()) {
            final StringBuilder faultyClasses = new StringBuilder();
            faulty.forEach(
                    clazz -> faultyClasses.append(clazz.getName()).append(" "));
            String exceptionMessage = String
                    .format("Classes [%s] given as navigation targets were not valid. "
                                    + "Add correct annotations or use RouteRegistry#"
                                    + "setRoute(String ,Class<? extends Component>, List<Class<? extends RouterLayout>>).",
                            faultyClasses.toString());
            throw new InvalidRouteConfigurationException(exceptionMessage);
        }
        registry.update(() -> {
            for (Class<? extends Component> navigationTarget : navigationTargets) {
                setRoute(navigationTarget, registry);
            }
        });
    }

    /**
     * Add the given navigation target as a route to the given route registry.
     *
     * @param navigationTarget
     *         navigation target to register
     * @param registry
     *         RouteRegistry to add route to
     * @throws InvalidRouteConfigurationException
     *         if a exact match route exists already
     */
    private static void setRoute(Class<? extends Component> navigationTarget,
            RouteRegistry registry) {

        String route = getNavigationRoute(navigationTarget);
        registry.setRoute(route, navigationTarget,
                RouteUtil.getParentLayouts(navigationTarget, route));

        for (String path : getRouteAliases(navigationTarget)) {
            registry.setRoute(path, navigationTarget,
                    RouteUtil.getParentLayouts(navigationTarget, path));
        }
    }

    /**
     * Giving a navigation target here will handle the {@link Route} annotation
     * to get the path and also register any {@link RouteAlias} that may be on
     * the class.
     * <p>
     * Note! A RouteAlias that is targeting an existing Route will throw.
     *
     * @param navigationTarget
     *         navigation target to register into the session route scope
     * @param registry
     *         RouteRegistry to add route to
     * @throws InvalidRouteConfigurationException
     *         thrown if exact route already defined in this scope
     */
    public static void setAnnotatedRoute(
            Class<? extends Component> navigationTarget,
            RouteRegistry registry) {
        setRoute(navigationTarget, registry);
    }

    /**
     * Register a navigation target on the specified path. Any {@link
     * ParentLayout} annotation on class will be used to populate layout chain,
     * but {@link Route} and {@link RouteAlias} will not be taken into
     * consideration.
     *
     * @param path
     *         path to register navigation target to
     * @param navigationTarget
     *         navigation target to register into the session route scope
     * @throws InvalidRouteConfigurationException
     *         thrown if exact route already defined in this scope
     */
    public static void setRoute(String path,
            Class<? extends Component> navigationTarget,
            RouteRegistry registry) {
        registry.setRoute(path, navigationTarget,
                RouteUtil.getParentLayoutsForNonRouteTarget(navigationTarget));
    }
}
