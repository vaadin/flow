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

/**
 * Utility class with methods for router layout handling.
 *
 * @deprecated This is an internal class for Flow and should not be used outside.
 * This is deprecated since 1.3 and will be removed in later versions of Flow.
 *
 * @since 1.0
 */
@Deprecated
public final class RouterUtil extends RouteUtil {

    private RouterUtil() {
    }

    /**
     * Get parent layouts for navigation target according to the {@link Route}
     * or {@link RouteAlias} annotation.
     *
     * @param component navigation target to get parents for
     * @param path      path used to get navigation target so we know which annotation
     *                  to handle
     * @return parent layouts for target
     */
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            Class<?> component, String path) {
        final List<Class<? extends RouterLayout>> list = new ArrayList<>();

        Optional<Route> route = AnnotationReader.getAnnotationFor(component,
                Route.class);
        List<RouteAlias> routeAliases = AnnotationReader
                .getAnnotationsFor(component, RouteAlias.class);
        if (route.isPresent()
                && path.equals(getRoutePath(component, route.get()))
                && !route.get().layout().equals(UI.class)) {
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
     * Updates the page title according to the currently visible component.
     * <p>
     * Uses {@link HasDynamicTitle#getPageTitle()} if implemented, or else the
     * {@link PageTitle} annotation, to resolve the title.
     *
     * @param navigationEvent the event object about the navigation
     * @param routeTarget     the currently visible component
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
     * @param routeTargetType  the actual component in the route chain
     * @param routeLayoutTypes the parent types in the route chain
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
     * @param event            current navigation event
     * @param routeTargetChain chain of route targets
     * @return new LocationChangeEvent
     */
    public static LocationChangeEvent createEvent(NavigationEvent event,
                                                  List<HasElement> routeTargetChain) {
        return new LocationChangeEvent(event.getSource(), event.getUI(),
                event.getTrigger(), event.getLocation(), routeTargetChain);
    }
}
