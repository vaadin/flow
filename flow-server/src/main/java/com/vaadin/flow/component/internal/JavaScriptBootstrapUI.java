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
package com.vaadin.flow.component.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.communication.JavaScriptBootstrapHandler;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Custom UI for {@link JavaScriptBootstrapHandler}. This class is intended for
 * internal use in clientSideMode.
 */
public
class JavaScriptBootstrapUI extends UI {
    private static final String NO_NAVIGATION = "Classic flow navigation is not supported for clien-side projects";

    private Element wrapperElement;

    /**
     * Connect a client with the server side UI.
     *
     * @param clientElementTag
     *            client side element tag
     * @param clientElementId
     *            client side element id
     * @param flowRoute
     *            flow route that should be attached to the client element
     */
    @ClientCallable
    public void connectClient(String clientElementTag, String clientElementId, String flowRoute) {

        // Get the flow view that the user wants to navigate to.
        final Element viewElement = getViewForRoute(flowRoute).getElement();

        if (wrapperElement == null) {
            // Create flow reference for the client outlet element
            wrapperElement = new Element(clientElementTag);

            // Connect server with client
            getElement().getStateProvider().appendVirtualChild(
                    getElement().getNode(), wrapperElement,
                    NodeProperties.INJECT_BY_ID, clientElementId);
        }

        // Remove previous view
        wrapperElement.removeAllChildren();
        // attach this view
        wrapperElement.appendChild(viewElement);

        // Inform the client, that everything went fine.
        wrapperElement.executeJs("$0.serverConnected()");
    }

    HasElement getViewForRoute(String route) {
        if (route.startsWith("/")) {
            route = route.replaceFirst("/+", "");
        }
        Location location = new Location(route);
        Optional<NavigationState> navigationState = this.getRouter()
                .resolveNavigationTarget(location);
        if (navigationState.isPresent()) {
            NavigationState currentState = navigationState.get();
            Class<? extends Component> routeTargetType = currentState
                    .getNavigationTarget();
            List<RouterLayout> layouts = getRouterLayouts(currentState,
                    routeTargetType);
            return getInternals().constructComponentWithLayouts(
                    getInstanceOf(routeTargetType), layouts);
        }

        return getFlowErrorComponent(location);
    }

    private List<RouterLayout> getRouterLayouts(NavigationState navigationState,
            Class<? extends Component> routeTargetType) {
        List<Class<? extends RouterLayout>> routeLayouts = this.getRouter()
                .getRegistry().getRouteLayouts(
                        navigationState.getResolvedPath(), routeTargetType);
        List<RouterLayout> layouts = new ArrayList<>();
        for (Class<? extends RouterLayout> routeLayout : routeLayouts) {
            layouts.add(getInstanceOf(routeLayout));
        }
        return layouts;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private HasElement getFlowErrorComponent(Location location) {
        HasElement errorComponent = createErrorComponentInstance();
        if (errorComponent instanceof HasErrorParameter) {
            // Create a dummy event to set error message
            BeforeEnterEvent beforeEnterEvent = new BeforeEnterEvent(
                    this.getRouter(), NavigationTrigger.PROGRAMMATIC, location,
                    errorComponent.getClass(), this, Collections.emptyList());
            String message = String.format("Route not found: '%s'",
                    location.getPath());
            ((HasErrorParameter) errorComponent).setErrorParameter(
                    beforeEnterEvent, new ErrorParameter<>(Exception.class,
                            new NotFoundException(message)));
        }
        return errorComponent;
    }

    private HasElement createErrorComponentInstance() {
        Optional<NavigationState> errorNavigationState = this.getRouter()
                .resolveRouteNotFoundNavigationTarget();
        if (!errorNavigationState.isPresent()) {
            // Default built-in RouteNotFoundError component
            return new RouteNotFoundError();
        } else {
            Class<? extends Component> errorNavigationTarget = errorNavigationState
                    .get().getNavigationTarget();
            return getInstanceOf(errorNavigationTarget);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends HasElement> T getInstanceOf(Class<T> routeTargetType) {
        Optional<HasElement> currentInstance = this.getInternals()
                .getActiveRouterTargetsChain().stream()
                .filter(component -> component.getClass()
                        .equals(routeTargetType))
                .findAny();
        return (T) currentInstance.orElseGet(
                () -> Instantiator.get(this).getOrCreate(routeTargetType));
    }

    @Override
    public Optional<ThemeDefinition> getThemeFor(Class<?> navigationTarget,
            String path) {
        return Optional.empty();
    }

    @Override
    public void navigate(String location) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    @Override
    public void navigate(Class<? extends Component> navigationTarget) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    @Override
    public <T, C extends Component & HasUrlParameter<T>> void navigate(
            Class<? extends C> navigationTarget, T parameter) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }

    @Override
    public void navigate(String location, QueryParameters queryParameters) {
        throw new UnsupportedOperationException(NO_NAVIGATION);
    }
}
