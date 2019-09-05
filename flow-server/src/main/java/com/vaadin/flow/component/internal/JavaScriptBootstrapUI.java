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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationStateBuilder;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.internal.ErrorStateRenderer;
import com.vaadin.flow.router.internal.NavigationStateRenderer;
import com.vaadin.flow.server.communication.JavaScriptBootstrapHandler;
import com.vaadin.flow.theme.ThemeDefinition;

/**
 * Custom UI for {@link JavaScriptBootstrapHandler}. This class is intended for
 * internal use in clientSideMode.
 */
public class JavaScriptBootstrapUI extends UI {
    private static final String NO_NAVIGATION = "Classic flow navigation is " +
            "not supported for client-side projects";

    private Element wrapperElement;

    /**
     * Create UI for clientSideMode.
     */
    public JavaScriptBootstrapUI() {
        super(new JavaScriptUIInternalsHandler());
    }
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

        if (wrapperElement == null) {
            // Create flow reference for the client outlet element
            wrapperElement = new Element(clientElementTag);

            // Connect server with client
            getElement().getStateProvider().appendVirtualChild(
                    getElement().getNode(), wrapperElement,
                    NodeProperties.INJECT_BY_ID, clientElementId);
        }
        // Render the flow view that the user wants to navigate to.
        renderViewForRoute(flowRoute);

        // Inform the client, that everything went fine.
        wrapperElement.executeJs("$0.serverConnected()");
    }

    /**
     * Get the wrapper element which is a container for server views in
     * client-side.
     * 
     * @return the wrapper element.
     */
    public Element getWrapperElement() {
        return wrapperElement;
    }

    private void renderViewForRoute(String route) {
        if (route.startsWith("/")) {
            route = route.replaceFirst("/+", "");
        }
        Location location = new Location(route);
        Optional<NavigationState> navigationState = this.getRouter()
                .resolveNavigationTarget(location);

        if (navigationState.isPresent()) {
            handleNavigation(location, navigationState.get());
        } else {
            handleErrorNavigation(location);
        }
    }

    private void handleNavigation(Location location,
            NavigationState navigationState) {
        NavigationEvent navigationEvent = new NavigationEvent(getRouter(),
                location, this, NavigationTrigger.CLIENT_SIDE);
        NavigationStateRenderer clientNavigationStateRenderer = new NavigationStateRenderer(
                navigationState);
        clientNavigationStateRenderer.handle(navigationEvent);
    }

    private void handleErrorNavigation(Location location) {
        NavigationState errorNavigationState = this.getRouter()
                .resolveRouteNotFoundNavigationTarget()
                .orElse(getDefaultNavigationError());
        ErrorStateRenderer errorStateRenderer = new ErrorStateRenderer(
                errorNavigationState);
        NotFoundException notFoundException = new NotFoundException(
                "Couldn't find route for '" + location.getPath() + "'");
        ErrorParameter<NotFoundException> errorParameter = new ErrorParameter<>(
                NotFoundException.class, notFoundException);
        ErrorNavigationEvent errorNavigationEvent = new ErrorNavigationEvent(
                this.getRouter(), location, this, NavigationTrigger.CLIENT_SIDE,
                errorParameter);
        errorStateRenderer.handle(errorNavigationEvent);
    }

    private NavigationState getDefaultNavigationError() {
        return new NavigationStateBuilder(this.getRouter())
                .withTarget(RouteNotFoundError.class).build();
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

    /**
     * An UIInternalsHandler implementation for clientSideMode.
     */
    private static class JavaScriptUIInternalsHandler
            implements UIInternalsHandler {

        @Override
        public void updateRoot(UI ui, HasElement oldRoot, HasElement newRoot) {
            JavaScriptBootstrapUI jsUI = castToJavaScriptUI(ui);
            // Remove previous view
            jsUI.getWrapperElement().removeAllChildren();
            // attach this view
            jsUI.getWrapperElement().appendChild(newRoot.getElement());
        }

        @Override
        public void moveToNewUI(UI oldUI, UI newUI) {
            JavaScriptBootstrapUI jsUI = castToJavaScriptUI(newUI);
            final List<Element> uiChildren = oldUI.getElement().getChildren()
                    .collect(Collectors.toList());
            jsUI.getWrapperElement().removeAllChildren();
            uiChildren.forEach(element -> {
                element.removeFromTree();
                jsUI.getWrapperElement().appendChild(element);
            });
        }

        private JavaScriptBootstrapUI castToJavaScriptUI(UI ui) {
            if (!(ui instanceof JavaScriptBootstrapUI)
                    || ((JavaScriptBootstrapUI) ui)
                            .getWrapperElement() == null) {
                throw new IllegalStateException("Can't update JavaScript UI "
                        + "because the current UI is not a JavaScript "
                        + "UI or wrapper element is null");
            }
            return (JavaScriptBootstrapUI) ui;
        }
    }
}
