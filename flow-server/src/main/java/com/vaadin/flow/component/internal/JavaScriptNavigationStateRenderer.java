/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationState;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.internal.NavigationStateRenderer;
import com.vaadin.flow.server.HttpStatusCode;

/**
 * Handle navigation events in relation to the client side bootstrap UI.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class JavaScriptNavigationStateRenderer extends NavigationStateRenderer {

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
        if (event.getUI().getInternals().getSession().getService()
                .getDeploymentConfiguration().isReactEnabled()) {
            return super.shouldPushHistoryState(event);
        }
        if (NavigationTrigger.CLIENT_SIDE.equals(event.getTrigger())
                && isPostponedClientSideNavigation()) {
            // When navigation is postponed, the legacy router does not update
            // the history, so it should be done on the server side when
            // proceeding.
            return true;
        }
        return super.shouldPushHistoryState(event);
    }

    @Override
    protected void pushHistoryState(NavigationEvent event) {
        super.pushHistoryState(event);

        if (isPostponedClientSideNavigation()) {
            event.getUI().navigateToClient(
                    event.getLocation().getPathWithQueryParameters());
        }
    }

    private boolean isPostponedClientSideNavigation() {
        return continueNavigationAction != null
                && UI.ClientViewPlaceholder.class.isAssignableFrom(
                        getNavigationState().getNavigationTarget());
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(JavaScriptNavigationStateRenderer.class.getName());
    }

}
