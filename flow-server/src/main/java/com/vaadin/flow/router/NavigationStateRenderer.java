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
package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.Route;
import com.vaadin.annotations.Title;
import com.vaadin.flow.router.event.ActivationState;
import com.vaadin.flow.router.event.BeforeNavigationEvent;
import com.vaadin.flow.router.event.BeforeNavigationListener;
import com.vaadin.flow.router.event.EventUtil;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasElement;
import com.vaadin.ui.UI;
import com.vaadin.util.ReflectTools;

/**
 * Handles navigation events by rendering a contained NavigationState in the
 * target UI.
 *
 * @see NavigationState
 * @see Route
 */
public class NavigationStateRenderer implements NavigationHandler {

    private final NavigationState navigationState;

    /**
     * Constructs a new NavigationStateRenderer that handles the given
     * navigation state.
     *
     * @param navigationState
     *            the navigation state handled by this instance
     */
    public NavigationStateRenderer(NavigationState navigationState) {
        this.navigationState = navigationState;
    }

    /**
     * Gets the component instance to use for the given type and the
     * corresponding navigation event.
     * <p>
     * Override this method to control the creation of view instances.
     * <p>
     * By default always creates new instances.
     *
     * @param <T>
     *            the route target type
     * @param routeTargetType
     *            the class of the route target component
     * @param event
     *            the navigation event that uses the route target
     * @return an instance of the route target component
     */
    @SuppressWarnings("unchecked")
    protected <T extends HasElement> T getRouteTarget(Class<T> routeTargetType,
            NavigationEvent event) {
        Optional<HasElement> currentInstance = event.getUI().getInternals()
                .getActiveRouterTargetsChain().stream()
                .filter(component -> component.getClass()
                        .equals(routeTargetType))
                .findAny();
        return (T) currentInstance
                .orElseGet(() -> ReflectTools.createInstance(routeTargetType));
    }

    @Override
    public int handle(NavigationEvent event) {
        UI ui = event.getUI();

        Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();
        List<Class<? extends RouterLayout>> routeLayoutTypes = getRouterLayoutTypes(
                event, routeTargetType);

        assert routeTargetType != null;
        assert routeLayoutTypes != null;

        checkForDuplicates(routeTargetType, routeLayoutTypes);

        BeforeNavigationEvent beforeNavigationDeactivating = new BeforeNavigationEvent(
                event, routeTargetType, ActivationState.DEACTIVATING);
        List<BeforeNavigationListener> listeners = EventUtil
                .collectBeforeNavigationListeners(ui.getElement());
        if (executeBeforeNavigation(beforeNavigationDeactivating, listeners)) {
            return reroute(event, beforeNavigationDeactivating);
        }

        Component componentInstance = getRouteTarget(routeTargetType, event);
        List<HasElement> chain = new ArrayList<>();
        chain.add(componentInstance);

        for (Class<? extends RouterLayout> parentType : routeLayoutTypes) {
            chain.add(getRouteTarget(parentType, event));
        }

        BeforeNavigationEvent beforeNavigationActivating = new BeforeNavigationEvent(
                event, routeTargetType,
                ActivationState.ACTIVATING);

        navigationState.getUrlParameters().ifPresent(urlParameters -> {
            HasUrlParameter hasUrlParameter = (HasUrlParameter) componentInstance;
            hasUrlParameter.setParameter(beforeNavigationActivating,
                    hasUrlParameter.deserializeUrlParameters(urlParameters));
        });

        listeners = EventUtil.collectBeforeNavigationListeners(chain);
        if (executeBeforeNavigation(beforeNavigationActivating, listeners)) {
            return reroute(event, beforeNavigationActivating);
        }

        @SuppressWarnings("unchecked")
        List<RouterLayout> routerLayouts = (List<RouterLayout>) (List<?>) chain
                .subList(1, chain.size());

        ui.getInternals().showRouteTarget(event.getLocation(),
                componentInstance, routerLayouts);

        updatePageTitle(event, routeTargetType, routeLayoutTypes);

        NewLocationChangeEvent locationChangeEvent = createEvent(event, chain);
        return locationChangeEvent.getStatusCode();
    }

    private boolean executeBeforeNavigation(
            BeforeNavigationEvent beforeNavigation,
            List<BeforeNavigationListener> listeners) {
        for (BeforeNavigationListener listener : listeners) {
            listener.beforeNavigation(beforeNavigation);

            if (beforeNavigation.hasRerouteTarget()) {
                return true;
            }
        }
        return false;
    }

    private int reroute(NavigationEvent event,
            BeforeNavigationEvent beforeNavigation) {
        NavigationHandler handler = beforeNavigation.getRerouteTarget();

        Location location = new Location(beforeNavigation.getRouteTargetType()
                .getAnnotation(Route.class).value());

        NavigationEvent newNavigationEvent = new NavigationEvent(
                event.getSource(), location, event.getUI(),
                NavigationTrigger.PROGRAMMATIC);

        return handler.handle(newNavigationEvent);
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
    protected static void checkForDuplicates(
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
     * Updates the page title according to the currently visible component.
     * <p>
     * Uses the {@link Title} to resolve the title.
     *
     * @param navigationEvent
     *            the event object about the navigation
     * @param routeTargetType
     *            the type of the route target
     */
    protected void updatePageTitle(NavigationEvent navigationEvent,
            Class<? extends Component> routeTargetType,
            List<Class<? extends RouterLayout>> routeLayoutTypes) {

        Title annotation = routeTargetType.getAnnotation(Title.class);
        if (annotation == null) {
            for (Class<?> clazz : routeLayoutTypes) {
                annotation = clazz.getAnnotation(Title.class);
                if (annotation != null) {
                    break;
                }
            }
        }
        if (annotation == null || annotation.value() == null) {
            navigationEvent.getUI().getPage().setTitle("");
        } else {
            navigationEvent.getUI().getPage().setTitle(annotation.value());
        }
    }

    private NewLocationChangeEvent createEvent(NavigationEvent event,
            List<HasElement> routeTargetChain) {
        return new NewLocationChangeEvent(event.getSource(), event.getUI(),
                event.getTrigger(), event.getLocation(), routeTargetChain);
    }

    private List<Class<? extends RouterLayout>> getRouterLayoutTypes(
            NavigationEvent event, Class<? extends Component> targetType) {
        assert targetType == navigationState.getNavigationTarget();
        Optional<Route> router = AnnotationReader.getAnnotationFor(targetType,
                Route.class);
        if (router.isPresent() && !router.get().layout().equals(UI.class)) {
            return Collections.singletonList(router.get().layout());
        }
        return Collections.emptyList();
    }
}
