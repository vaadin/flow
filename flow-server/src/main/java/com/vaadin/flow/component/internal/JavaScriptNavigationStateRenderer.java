/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.internal;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.internal.NavigationStateRenderer;
import com.vaadin.flow.server.HttpStatusCode;

/**
 * Handle navigation events in relation to the client side bootstrap UI
 * {@link JavaScriptBootstrapUI}.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class JavaScriptNavigationStateRenderer extends NavigationStateRenderer {

    static final String NOT_SUPPORT_FORWARD_BEFORELEAVE = "BeforeLeaveEvent.forwardTo() is not supported. "
            + "You can use the combination between BeforeLeaveEvent.postpone() and "
            + "UI.getPage().setLocation(\"{}\") "
            + "in order to forward to other location";

    static final String NOT_SUPPORT_REROUTE = "BeforeEvent.rerouteTo() with a client side route is not supported";

    private String clientForwardRoute;

    private ContinueNavigationAction continueNavigationAction;

    /**
     * Constructs a new NavigationStateRenderer that handles the given
     * navigation state.
     *
     * @param navigationState
     *            the navigation state handled by this instance
     */
    public JavaScriptNavigationStateRenderer(NavigationState navigationState) {
        super(navigationState);
    }

    /**
     * Gets the client forward route.
     *
     * @return the client forward route.
     */
    public String getClientForwardRoute() {
        return clientForwardRoute;
    }

    @Override
    public int handle(NavigationEvent event) {

        continueNavigationAction = event.getUI().getInternals()
                .getContinueNavigationAction();

        return super.handle(event);
    }

    @Override
    protected Optional<Integer> handleTriggeredBeforeEvent(
            NavigationEvent event, BeforeEvent beforeEvent) {
        if (beforeEvent.hasUnknownForward()) {
            if (beforeEvent instanceof BeforeLeaveEvent) {
                getLogger().warn(NOT_SUPPORT_FORWARD_BEFORELEAVE,
                        beforeEvent.getUnknownForward());

            } else {
                clientForwardRoute = beforeEvent.getUnknownForward();
                return Optional.of(HttpStatusCode.OK.getCode());
            }
        }

        if (beforeEvent.hasUnknownReroute()) {
            getLogger().warn(NOT_SUPPORT_REROUTE);
        }

        return super.handleTriggeredBeforeEvent(event, beforeEvent);
    }

    @Override
    protected boolean shouldPushHistoryState(NavigationEvent event) {
        if (NavigationTrigger.CLIENT_SIDE.equals(event.getTrigger())) {
            return true;
        } else {
            return super.shouldPushHistoryState(event);
        }
    }

    @Override
    protected void pushHistoryState(NavigationEvent event) {
        super.pushHistoryState(event);

        if (event.getUI() instanceof JavaScriptBootstrapUI) {
            JavaScriptBootstrapUI jsUI = (JavaScriptBootstrapUI) event.getUI();

            if (continueNavigationAction != null
                    // We're trying to navigate to a client view.
                    && JavaScriptBootstrapUI.ClientViewPlaceholder.class
                            .isAssignableFrom(getNavigationState()
                                    .getNavigationTarget())) {
                jsUI.navigateToClient(
                        event.getLocation().getPathWithQueryParameters());
            }
        }

    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(JavaScriptNavigationStateRenderer.class.getName());
    }

}
