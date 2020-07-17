/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.router.ErrorNavigationEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationStateBuilder;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.internal.ErrorStateRenderer;
import com.vaadin.flow.router.internal.ErrorTargetEntry;
import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.communication.JavaScriptBootstrapHandler;

/**
 * Custom UI for {@link JavaScriptBootstrapHandler}. This class is intended for
 * internal use in client side bootstrapping.
 */
public class JavaScriptBootstrapUI extends UI {
    public static final String SERVER_ROUTING = "clientRoutingMode";

    static final String SERVER_CONNECTED = "this.serverConnected($0)";
    static final String CLIENT_NAVIGATE_TO = "window.dispatchEvent(new CustomEvent('vaadin-router-go', {detail: new URL($0, document.baseURI)}))";

    Element wrapperElement;
    private NavigationState clientViewNavigationState;
    private boolean navigationInProgress = false;

    private String forwardToClientUrl = null;

    /**
     * Create UI for client side bootstrapping.
     */
    public JavaScriptBootstrapUI() {
        super(new JavaScriptUIInternalUpdater());
    }

    @Override
    public Stream<Component> getChildren() {
        // server-side routing
        if (wrapperElement == null) {
            return super.getChildren();
        }

        // client-side routing,
        // since virtual child is used, it is necessary to change the original
        // UI element to the wrapperElement
        Builder<Component> childComponents = Stream.builder();
        wrapperElement.getChildren().forEach(childElement -> ComponentUtil
                .findComponents(childElement, childComponents::add));
        return childComponents.build();
    }

    /**
     * Gets the new forward url.
     *
     * @return the new forward url
     */
    public String getForwardToClientUrl() {
        return forwardToClientUrl;
    }

    /**
     * Connect a client with the server side UI. This method is invoked each
     * time client router navigates to a server route.
     *
     * @param clientElementTag
     *            client side element tag
     * @param clientElementId
     *            client side element id
     * @param flowRoute
     *            flow route that should be attached to the client element
     */
    @ClientCallable
    public void connectClient(String clientElementTag, String clientElementId,
            String flowRoute) {
        if (wrapperElement == null) {
            // Create flow reference for the client outlet element
            wrapperElement = new Element(clientElementTag);

            // Connect server with client
            getElement().getStateProvider().appendVirtualChild(
                    getElement().getNode(), wrapperElement,
                    NodeProperties.INJECT_BY_ID, clientElementId);
        }

        final String trimmedRoute = PathUtil.trimPath(flowRoute);
        if (!trimmedRoute.equals(flowRoute)) {
            // See InternalRedirectHandler invoked via Router.
            getPage().getHistory().replaceState(null, trimmedRoute);
        }

        // Render the flow view that the user wants to navigate to.
        renderViewForRoute(new Location(trimmedRoute),
                NavigationTrigger.CLIENT_SIDE);

        // true if the target is client-view and the push mode is disable
        if (getForwardToClientUrl() != null) {
            navigateToClient(getForwardToClientUrl());
            acknowledgeClient();
            
        } else if (isPostponed()) {
            cancelClient();
        } else {
            acknowledgeClient();
        }

        // If this call happens, there is a client-side routing, thus
        // it's needed to remove the flag that might be set in
        // IndexHtmlRequestHandler
        getSession().setAttribute(SERVER_ROUTING, Boolean.FALSE);
    }

    /**
     * Check that the view can be leave. This method is invoked when the client
     * router tries to navigate to a client route while the current route is a
     * server route.
     *
     * This is only called when client route navigates from a server to a client
     * view.
     *
     * @param route
     *            the route that is navigating to.
     */
    @ClientCallable
    public void leaveNavigation(String route) {
        navigateToPlaceholder(new Location(PathUtil.trimPath(route)));

        // Inform the client whether the navigation should be postponed
        if (isPostponed()) {
            cancelClient();
        } else {
            acknowledgeClient();
        }
    }

    /*
     * Navigate to a path triggered by a server view.
     */
    @Override
    public void navigate(String pathname, QueryParameters queryParameters) {
        Location location = new Location(pathname, queryParameters);
        if (Boolean.TRUE.equals(getSession().getAttribute(SERVER_ROUTING))) {
            // server-side routing
            renderViewForRoute(location, NavigationTrigger.UI_NAVIGATE);
        } else {
            // client-side routing

            // There is an in-progress navigation or there are no changes,
            // prevent looping
            if (navigationInProgress || getInternals().hasLastHandledLocation()
                    && sameLocation(getInternals().getLastHandledLocation(),
                    location)) {
                return;
            }

            navigationInProgress = true;
            Optional<NavigationState>  navigationState = this.getRouter()
                    .resolveNavigationTarget(location);

            if (navigationState.isPresent()) {
                // Navigation can be done in server side without extra
                // round-trip
                handleNavigation(location, navigationState.get(),
                        NavigationTrigger.UI_NAVIGATE);
                if (getForwardToClientUrl() != null) {
                    navigationInProgress = false;
                    // Server is forwarding to a client route from a
                    // BeforeEnter.
                    navigateToClient(getForwardToClientUrl());
                    return;
                }
            } else {
                // Server cannot resolve navigation, let client-side to handle
                // it.
                navigateToClient(location.getPathWithQueryParameters());
            }

            navigationInProgress = false;
        }
    }

    void navigateToClient(String clientRoute) {
        getPage().executeJs(CLIENT_NAVIGATE_TO, clientRoute);
    }

    private void acknowledgeClient() {
        serverConnected(false);
    }
    
    private void cancelClient() {
        serverConnected(true);
    }

    private void serverConnected(boolean cancel) {
        wrapperElement.executeJs(SERVER_CONNECTED, cancel);
    }

    private void navigateToPlaceholder(Location location) {
        if (clientViewNavigationState == null) {
            clientViewNavigationState = new NavigationStateBuilder(
                    this.getRouter()).withTarget(ClientViewPlaceholder.class)
                    .build();
        }
        // Passing the `clientViewLocation` to make sure that the navigation
        // events contain the correct location that we are navigating to.
        handleNavigation(location, clientViewNavigationState,
                NavigationTrigger.CLIENT_SIDE);
    }

    private void renderViewForRoute(Location location, NavigationTrigger trigger) {
        if (!shouldHandleNavigation(location)) {
            return;
        }
        try {
            getInternals().setLastHandledNavigation(location);
            Optional<NavigationState> navigationState = this.getRouter()
                    .resolveNavigationTarget(location);
            if (navigationState.isPresent()) {
                // There is a valid route in flow.
                handleNavigation(location, navigationState.get(), trigger);
            } else {
                // When route does not exist, try to navigate to current route
                // in order to check if current view can be left before showing
                // the error page
                navigateToPlaceholder(location);

                if (!isPostponed()) {
                    // Route does not exist, and current view does not prevent
                    // navigation thus an error page is shown
                    handleErrorNavigation(location);
                }

            }
        } catch (Exception exception) {
            handleExceptionNavigation(location, exception);
        } finally {
            getInternals().clearLastHandledNavigation();
        }
    }

    private boolean shouldHandleNavigation(Location location) {
        return !getInternals().hasLastHandledLocation()
                || !sameLocation(getInternals().getLastHandledLocation(),
                        location);
    }

    private boolean sameLocation(Location oldLocation, Location newLocation) {
        return PathUtil.trimPath(newLocation.getPathWithQueryParameters())
                .equals(PathUtil
                        .trimPath(oldLocation.getPathWithQueryParameters()));
    }

    private void handleNavigation(Location location,
            NavigationState navigationState, NavigationTrigger trigger) {
        NavigationEvent navigationEvent = new NavigationEvent(getRouter(),
                location, this, trigger);

        JavaScriptNavigationStateRenderer clientNavigationStateRenderer = new JavaScriptNavigationStateRenderer(
                navigationState);

        clientNavigationStateRenderer.handle(navigationEvent);

        forwardToClientUrl = clientNavigationStateRenderer.getClientForwardRoute();

        adjustPageTitle();
    }

    private boolean handleExceptionNavigation(Location location,
            Exception exception) {
        Optional<ErrorTargetEntry> maybeLookupResult = this.getRouter()
                .getErrorNavigationTarget(exception);
        if (maybeLookupResult.isPresent()) {
            ErrorTargetEntry lookupResult = maybeLookupResult.get();

            ErrorParameter<?> errorParameter = new ErrorParameter<>(
                    lookupResult.getHandledExceptionType(), exception,
                    exception.getMessage());
            ErrorStateRenderer errorStateRenderer = new ErrorStateRenderer(
                    new NavigationStateBuilder(this.getRouter())
                            .withTarget(lookupResult.getNavigationTarget())
                            .build());

            ErrorNavigationEvent errorNavigationEvent = new ErrorNavigationEvent(
                    this.getRouter(), location, this,
                    NavigationTrigger.CLIENT_SIDE, errorParameter);

            errorStateRenderer.handle(errorNavigationEvent);
        } else {
            throw new RuntimeException(exception);
        }
        return isPostponed();
    }

    private boolean isPostponed() {
        return getInternals().getContinueNavigationAction() != null;
    }

    private void adjustPageTitle() {
        // new title is empty if the flow route does not have a title
        String newTitle = getInternals().getTitle();
        // app shell title is computed from the title tag in index.html
        String appTitle = AppShellRegistry
                .getInstance(VaadinService.getCurrent().getContext())
                .getTitle();
        // restore the app shell title when there is no one for the route
        if ((newTitle == null || newTitle.isEmpty()) && !appTitle.isEmpty()) {
            getInternals().cancelPendingTitleUpdate();
            getInternals().setTitle(appTitle);
        }
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

    /**
     * An UIInternalsHandler implementation for client side bootstrapping.
     */
    private static class JavaScriptUIInternalUpdater
            implements UIInternalUpdater {

        @Override
        public void updateRoot(UI ui, HasElement oldRoot, HasElement newRoot) {
            JavaScriptBootstrapUI jsUI = castToJavaScriptUI(ui);
            Element wrapperElement = jsUI.wrapperElement;
            // server-side routing
            if (wrapperElement == null) {
                UIInternalUpdater.super.updateRoot(ui, oldRoot, newRoot);
                return;
            }

            // client-side routing
            Element rootElement = newRoot.getElement();
            if (newRoot instanceof ClientViewPlaceholder) {
                // only need to remove all children when newRoot is a
                // placeholder
                wrapperElement.removeAllChildren();
            } else if (!wrapperElement.equals(rootElement.getParent())) {
                if (oldRoot != null) {
                    oldRoot.getElement().removeFromParent();
                }
                rootElement.removeFromParent();
                wrapperElement.appendChild(rootElement);
            }
        }

        @Override
        public void moveToNewUI(UI oldUI, UI newUI) {
            JavaScriptBootstrapUI newJsUI = castToJavaScriptUI(newUI);
            JavaScriptBootstrapUI oldJsUI = castToJavaScriptUI(oldUI);

            Element oldRoot = oldJsUI.getElement();
            Element newRoot = newJsUI.getElement();

            oldRoot.getChildren().collect(Collectors.toList())
                    .forEach(element -> {
                        element.removeFromTree();
                        newRoot.appendChild(element);
                    });
        }

        private JavaScriptBootstrapUI castToJavaScriptUI(UI ui) {
            assert ui instanceof JavaScriptBootstrapUI;
            return (JavaScriptBootstrapUI) ui;
        }
    }

    /**
     * Placeholder view when navigating from server-side views to client-side
     * views.
     */
    @Tag(Tag.DIV)
    public static class ClientViewPlaceholder extends Component {
    }
}
