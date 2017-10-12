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
package com.vaadin.router;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.router.event.ActivationState;
import com.vaadin.router.event.AfterNavigationEvent;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.BeforeNavigationListener;
import com.vaadin.router.event.ErrorNavigationEvent;
import com.vaadin.router.event.EventUtil;
import com.vaadin.router.event.NavigationEvent;
import com.vaadin.router.util.RouterUtil;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.common.HasElement;

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
        UI ui = event.getUI();
        Optional<HasElement> currentInstance = ui.getInternals()
                .getActiveRouterTargetsChain().stream()
                .filter(component -> component.getClass()
                        .equals(routeTargetType))
                .findAny();
        return (T) currentInstance.orElseGet(() -> Instantiator.get(ui)
                .createRouteTarget(routeTargetType, event));
    }

    @Override
    public int handle(NavigationEvent event) {
        UI ui = event.getUI();

        Class<? extends Component> routeTargetType = navigationState
                .getNavigationTarget();
        List<Class<? extends RouterLayout>> routeLayoutTypes = getRouterLayoutTypes(
                routeTargetType);

        assert routeTargetType != null;
        assert routeLayoutTypes != null;

        RouterUtil.checkForDuplicates(routeTargetType, routeLayoutTypes);

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
                event, routeTargetType, ActivationState.ACTIVATING);

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

        RouterUtil.updatePageTitle(event, componentInstance);

        LocationChangeEvent locationChangeEvent = RouterUtil.createEvent(event,
                chain);

        if (locationChangeEvent.getStatusCode() == HttpServletResponse.SC_OK) {
            fireAfterNavigationListeners(chain,
                    new AfterNavigationEvent(locationChangeEvent));
        }

        if (componentInstance instanceof RouteNotFoundError) {
            locationChangeEvent.setStatusCode(HttpServletResponse.SC_NOT_FOUND);
        }
        return locationChangeEvent.getStatusCode();
    }

    private void fireAfterNavigationListeners(List<HasElement> chain,
            AfterNavigationEvent event) {
        EventUtil.collectAfterNavigationListeners(chain)
                .forEach(listener -> listener.afterNavigation(event));
    }

    /**
     * Gets the router layout types to show for the given route target type,
     * starting from the parent layout immediately wrapping the route target
     * type.
     *
     * @param targetType
     *            component type to show
     *
     * @return a list of parent {@link RouterLayout} types, not
     *         <code>null</code>
     */
    public List<Class<? extends RouterLayout>> getRouterLayoutTypes(
            Class<? extends Component> targetType) {
        assert targetType == navigationState.getNavigationTarget();

        return RouterUtil.getParentLayouts(targetType);
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

        NavigationEvent newNavigationEvent = getNavigationEvent(event,
                beforeNavigation);

        return handler.handle(newNavigationEvent);
    }

    private NavigationEvent getNavigationEvent(NavigationEvent event,
            BeforeNavigationEvent beforeNavigation) {
        if (beforeNavigation.hasErrorParameter()) {
            ErrorParameter<?> errorParameter = beforeNavigation
                    .getErrorParameter();
            Location location = new Location(
                    errorParameter.getException().getClass().getSimpleName());

            return new ErrorNavigationEvent(event.getSource(), location,
                    event.getUI(), NavigationTrigger.PROGRAMMATIC,
                    errorParameter);
        }

        Location location = new Location(beforeNavigation.getRouteTargetType()
                .getAnnotation(Route.class).value());

        return new NavigationEvent(event.getSource(), location, event.getUI(),
                NavigationTrigger.PROGRAMMATIC);
    }
}
