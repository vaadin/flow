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
package com.vaadin.router.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.vaadin.router.HasDynamicTitle;
import com.vaadin.router.LocationChangeEvent;
import com.vaadin.router.PageTitle;
import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.router.RouteAlias;
import com.vaadin.router.RouterLayout;
import com.vaadin.router.event.NavigationEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.HasElement;
import com.vaadin.util.AnnotationReader;

/**
 * Utility class with methods for router layout handling.
 */
public final class RouterUtil {

    /**
     * Get parent layouts for navigation target {@link Route} annotation.
     *
     * @param component
     *            navigation target to get parents for
     * @return parent layouts for target
     */
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            Class<?> component) {
        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);
        if (route.isPresent() && !route.get().layout().equals(UI.class)) {
            return collectRouteParentLayouts(route.get().layout());
        }
        return new ArrayList<>(0);
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
     */
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            Class<?> component, String path) {
        final List<Class<? extends RouterLayout>> list = new ArrayList<>();

        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);
        List<RouteAlias> routeAliases = AnnotationReader
                .getAnnotationsFor(component, RouteAlias.class);
        if (route.isPresent() && path.equals(route.get().value())
                && !route.get().layout().equals(UI.class)) {
            list.addAll(collectRouteParentLayouts(route.get().layout()));
        } else {

            Optional<RouteAlias> matchingRoute = getMatchingRouteAlias(path,
                    routeAliases);
            if (matchingRoute.isPresent()) {
                list.addAll(collectRouteParentLayouts(
                        matchingRoute.get().layout()));
            }
        }

        return list;
    }

    private static Optional<RouteAlias> getMatchingRouteAlias(String path,
            List<RouteAlias> routeAliases) {
        return routeAliases.stream().filter(alias -> path.equals(alias.value())
                && !alias.layout().equals(UI.class)).findFirst();
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
     * Get the top most parent layout for navigation target {@link Route}
     * annotation.
     *
     * @param component
     *            navigation target to get top most parent for
     * @return top parent layout for target or null if none found
     */
    public static Class<? extends RouterLayout> getTopParentLayout(
            Class<?> component) {
        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);
        if (route.isPresent() && !route.get().layout().equals(UI.class)) {
            return recuseToTopLayout(route.get().layout());
        }
        return null;
    }

    /**
     * Get the top most parent layout for navigation target according to the
     * {@link Route} or {@link RouteAlias} annotation.
     *
     * @param component
     *            navigation target to get top most parent for
     * @param path
     *            path used to get navigation target so we know which annotation
     *            to handle
     * @return top parent layout for target or null if none found
     */
    public static Class<? extends RouterLayout> getTopParentLayout(
            final Class<?> component, final String path) {
        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);
        List<RouteAlias> routeAliases = AnnotationReader
                .getAnnotationsFor(component, RouteAlias.class);
        if (route.isPresent() && path.equals(route.get().value())
                && !route.get().layout().equals(UI.class)) {
            return recuseToTopLayout(route.get().layout());
        } else {
            Optional<RouteAlias> matchingRoute = getMatchingRouteAlias(path,
                    routeAliases);
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
     *            the event object about the navigation
     * @param routeTarget
     *            the currently visible component
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
     *            the actual component in the route chain
     * @param routeLayoutTypes
     *            the parent types in the route chain
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
     *            current navigation event
     * @param routeTargetChain
     *            chain of route targets
     * @return new LocationChangeEvent
     */
    public static LocationChangeEvent createEvent(NavigationEvent event,
            List<HasElement> routeTargetChain) {
        return new LocationChangeEvent(event.getSource(), event.getUI(),
                event.getTrigger(), event.getLocation(), routeTargetChain);
    }
}
