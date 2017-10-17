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
     * Get parent layouts for navigation target.
     * 
     * @param component
     *            navigation target to get parents for
     * @return parent layouts for target
     */
    public static List<Class<? extends RouterLayout>> getParentLayouts(
            Class<?> component) {
        List<Class<? extends RouterLayout>> list = new ArrayList<>();

        Optional<Route> router = AnnotationReader.getAnnotationFor(component,
                Route.class);
        Optional<ParentLayout> parentLayout = AnnotationReader
                .getAnnotationFor(component, ParentLayout.class);

        if (router.isPresent() && !router.get().layout().equals(UI.class)) {
            list.add(router.get().layout());
            list.addAll(getParentLayouts(router.get().layout()));
        } else if (parentLayout.isPresent()) {
            list.add(parentLayout.get().value());
            list.addAll(getParentLayouts(parentLayout.get().value()));
        }

        return list;
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
