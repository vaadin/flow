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
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;

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
     *            navigation target to get parents for
     * @return parent layouts for target
     *
     * @deprecated This is internal method and will be removed in the future
     *             since it's not longer needed
     */
    @Deprecated
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            Class<?> component) {
        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);
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
     *            navigation target to get parents for
     * @param path
     *            path used to get navigation target so we know which annotation
     *            to handle
     * @return parent layouts for target
     *
     * @deprecated Use
     *             {@link RouteConfiguration#discoverParentLayouts(Class, String)}
     */
    @Deprecated
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            Class<?> component, String path) {
        return RouteConfiguration.forApplicationScope()
                .discoverParentLayouts(component, path);
    }

    /**
     * Get the actual route path including all parent layout
     * {@link RoutePrefix}.
     *
     * @param component
     *            navigation target component to get route path for
     * @param route
     *            route annotation to check
     * @return actual path for given route target
     *
     * @deprecated Use {@link RouteConfiguration#getRoutePath(Class, Route)}
     */
    @Deprecated
    public static String getRoutePath(Class<?> component, Route route) {
        return RouteConfiguration.forApplicationScope().getRoutePath(component,
                route);
    }

    /**
     * Get the actual route path including all parent layout
     * {@link RoutePrefix}.
     *
     * @param component
     *            navigation target component to get route path for
     * @param alias
     *            route alias annotation to check
     * @return actual path for given route alias target
     *
     * @deprecated Use
     *             {@link RouteConfiguration#getRouteAliasPath(Class, RouteAlias)}
     */
    @Deprecated
    public static String getRouteAliasPath(Class<?> component,
            RouteAlias alias) {
        return RouteConfiguration.forApplicationScope()
                .getRouteAliasPath(component, alias);
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
     *            route to check parent layouts for
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
     *            navigation target to get top most parent for
     * @param path
     *            path used to get navigation target so we know which annotation
     *            to handle or null for error views.
     * @return top parent layout for target or null if none found
     *
     * @deprecated Use
     *             {@link RouteConfiguration#getTopParentLayout(Class, String)}
     */
    @Deprecated
    public static Class<? extends RouterLayout> getTopParentLayout(
            final Class<?> component, final String path) {
        return RouteConfiguration.forApplicationScope().getTopParentLayout(
                (Class<? extends Component>) component, path);
    }

    /**
     * Updates the page title according to the currently visible component.
     * <p>
     * Uses {@link HasDynamicTitle#getPageTitle()} if implemented, or else the
     * {@link PageTitle} annotation, to resolve the title.
     *
     * @param navigationEvent
     *            the event object about the navigation
     * @param routeTarget
     *            the currently visible component
     * @deprecated This is internal method and will be removed in the future
     *             since it's not longer needed
     */
    @Deprecated
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

    /**
     *
     * XXX: remove along with
     * {@link #updatePageTitle(NavigationEvent, Component)}
     */
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
     *            the actual component in the route chain
     * @param routeLayoutTypes
     *            the parent types in the route chain
     *
     * @deprecated This is internal method and will be removed in the future
     *             since it's not longer needed
     */
    @Deprecated
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
     *            current navigation event
     * @param routeTargetChain
     *            chain of route targets
     * @return new LocationChangeEvent
     *
     * @deprecated This is internal method and will be removed in the future
     *             since it's not longer needed
     */
    @Deprecated
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
     *            navigation target to get chain route for
     * @return full navigation route
     *
     * @deprecated This is internal method and will be removed in the future
     *             since it's not longer needed
     */
    @Deprecated
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
     *            navigation target to get chain route alias for
     * @return full navigation route
     *
     * @deprecated This is internal method and will be removed in the future
     *             since it's not longer needed
     */
    @Deprecated
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
     * Gets the effective route path value of the annotated class.
     *
     * @param component
     *            the component where the route points to
     * @param route
     *            the annotation
     * @return The value of the annotation or naming convention based value if
     *         no explicit value is given.
     */
    public static String resolve(Class<?> component, Route route) {
        if (route.value().equals(Route.NAMING_CONVENTION)) {
            String simpleName = component.getSimpleName();
            if ("MainView".equals(simpleName) || "Main".equals(simpleName)) {
                return "";
            }
            if (simpleName.endsWith("View")) {
                return simpleName
                        .substring(0, simpleName.length() - "View".length())
                        .toLowerCase();
            }
            return simpleName.toLowerCase();
        }
        return route.value();
    }
}
