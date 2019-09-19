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
package com.vaadin.flow.router;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.internal.NavigationStateRenderer;

/**
 * Event created when the location changes by any of the reasons defined at
 * {@link NavigationTrigger}.
 *
 * @since 1.0
 */
public class LocationChangeEvent extends EventObject {
    private final UI ui;
    private final NavigationTrigger trigger;
    private final Location location;

    private int statusCode = HttpServletResponse.SC_OK;
    private NavigationHandler rerouteTarget;

    private List<HasElement> routeTargetChain;

    /**
     * Creates a new location change event.
     *
     * @param router
     *            the router that triggered the change, not {@code null}
     * @param ui
     *            the UI in which the view is used, not {@code null}
     * @param trigger
     *            the type of user action that triggered this location change,
     *            not <code>null</code>
     * @param routeTargetChain
     *            the route terget chain that will be used, not {@code null}
     * @param location
     *            the new location, not {@code null}
     */
    public LocationChangeEvent(Router router, UI ui, NavigationTrigger trigger,
            Location location, List<HasElement> routeTargetChain) {
        super(router);

        assert ui != null;
        assert trigger != null;
        assert location != null;

        this.ui = ui;
        this.trigger = trigger;
        this.location = location;
        this.routeTargetChain = Collections.unmodifiableList(routeTargetChain);
    }

    /**
     * Gets the new location.
     *
     * @return the new location, not {@code null}
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the chain of route targets that will be nested inside the UI,
     * starting from the most deeply nested component.
     *
     * @return the view chain, not {@code null}
     */
    public List<HasElement> getRouteTargetChain() {
        return routeTargetChain;
    }

    /**
     * Gets the UI in which the view is shown.
     *
     * @return the UI, not {@code null}
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Gets the type of user action that triggered this location change.
     *
     * @return the type of user action that triggered this location change, not
     *         <code>null</code>
     */
    public NavigationTrigger getTrigger() {
        return trigger;
    }

    /**
     * Gets the query parameters used for navigation. If only the first value of
     * parameter list is important, please use
     * {@link LocationChangeEvent#getQueryParameter(String)}
     *
     * @return the query parameters, not {@code null}
     */
    public Map<String, List<String>> getQueryParameters() {
        return location.getQueryParameters().getParameters();
    }

    /**
     * Gets first parameter that corresponds to specified {@code parameterName}.
     * If there are multiple parameters corresponding to the same
     * {@code parameterName}, the first one will be returned. To access all
     * parameters, use {@link LocationChangeEvent#getQueryParameters()} method.
     *
     * @param parameterName
     *            the name of a parameter to get
     * @return first corresponding query parameter or {@link Optional#empty()},
     *         if no parameters found for {@code parameterName} specified
     */
    public Optional<String> getQueryParameter(String parameterName) {
        return location.getQueryParameters().getParameters()
                .getOrDefault(parameterName, Collections.emptyList()).stream()
                .findFirst();
    }

    @Override
    public Router getSource() {
        return (Router) super.getSource();
    }

    /**
     * Gets the HTTP status code that will be returned for the client if this
     * location change is an initial rendering request.
     *
     * @return the http status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP status code that will be returned for the client if this
     * location change is an initial rendering request.
     *
     * @param statusCode
     *            the http status code
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the reroute target to use if the user should be rerouted to some
     * other view.
     *
     * @return and optional navigation handler, or an empty optional if no
     *         reroute target has been set
     */
    public Optional<NavigationHandler> getRerouteTarget() {
        return Optional.ofNullable(rerouteTarget);
    }

    /**
     * Reroutes the navigation to use the provided navigation handler instead of
     * the currently used handler.
     *
     * @param rerouteTarget
     *            the navigation handler to use, or {@code null} to clear a
     *            previously set reroute target
     */
    public void rerouteTo(NavigationHandler rerouteTarget) {
        this.rerouteTarget = rerouteTarget;
    }

    /**
     * Reroutes the navigation to show the given component instead of the
     * component that is currently about to be displayed.
     *
     * @param rerouteTargetState
     *            the target navigation state of the rerouting, not {@code null}
     */
    public void rerouteTo(NavigationState rerouteTargetState) {
        Objects.requireNonNull(rerouteTargetState,
                "rerouteTargetState cannot be null");
        rerouteTo(new NavigationStateRenderer(rerouteTargetState));
    }
}
