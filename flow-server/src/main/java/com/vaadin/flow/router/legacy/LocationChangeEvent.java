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
package com.vaadin.flow.router.legacy;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.RouterInterface;

/**
 * Event passed to {@link View#onLocationChange(LocationChangeEvent)} when the
 * context in which a view will be used changes.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class LocationChangeEvent extends EventObject {
    private final UI ui;
    private final NavigationTrigger trigger;
    private final Location location;
    private final List<View> viewChain;
    private final Map<String, String> routePlaceholders;

    private int statusCode = HttpServletResponse.SC_OK;
    private NavigationHandler rerouteTarget;

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
     * @param location
     *            the new location, not {@code null}
     * @param viewChain
     *            the view chain that will be used, not {@code null}
     * @param routePlaceholders
     *            a map containing actual path segment values used for
     *            placeholders in the used route mapping, not {@code null}
     */
    public LocationChangeEvent(RouterInterface router, UI ui,
            NavigationTrigger trigger,
            Location location, List<View> viewChain,
            Map<String, String> routePlaceholders) {
        super(router);

        assert ui != null;
        assert trigger != null;
        assert location != null;
        assert viewChain != null;
        assert routePlaceholders != null;

        this.ui = ui;
        this.trigger = trigger;
        this.location = location;
        this.viewChain = Collections.unmodifiableList(viewChain);
        this.routePlaceholders = Collections.unmodifiableMap(routePlaceholders);
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
     * Gets the view which is being shown.
     * <p>
     * This is the same as the most deeply nested view in the view chain.
     *
     * @return the view being shown, not {@code null}
     */
    public View getView() {
        return viewChain.get(0);
    }

    /**
     * Gets the chain of views that will be nested inside the UI, starting from
     * the most deeply nested view.
     *
     * @return the view chain, not {@code null}
     */
    public List<View> getViewChain() {
        return viewChain;
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
     * Gets the part of the location that matched the <code>*</code> part of the
     * route.
     *
     * @return the wildcard part of the path, or {@code null} if not using a
     *         wildcard route
     */
    public String getPathWildcard() {
        return getPathParameter("*");
    }

    /**
     * Gets the part of the location that matched <code>{placeholderName}</code>
     * of the route.
     *
     * @param placeholderName
     *            the name of the placeholder, not {@code null}
     * @return the placeholder value, or {@code null} if the placeholder name
     *         was not present in the route
     */
    public String getPathParameter(String placeholderName) {
        assert placeholderName != null;
        assert !placeholderName.contains("{") && !placeholderName.contains("}");

        return routePlaceholders.get(placeholderName);
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
    public RouterInterface getSource() {
        return (RouterInterface) super.getSource();
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
     * <p>
     * Calling this method outside
     * {@link View#onLocationChange(LocationChangeEvent)} will not have any
     * effect.
     *
     * @param rerouteTarget
     *            the navigation handler to use, or {@code null} to clear a
     *            previously set reroute target
     */
    public void rerouteTo(NavigationHandler rerouteTarget) {
        this.rerouteTarget = rerouteTarget;
    }

    /**
     * Reroutes the navigation to show the default error view instead of the
     * view that is currently about to be displayed.
     *
     * <p>
     * Calling this method outside
     * {@link View#onLocationChange(LocationChangeEvent)} will not have any
     * effect.
     */
    public void rerouteToErrorView() {
        rerouteTo(getSource().getConfiguration().getErrorHandler());
    }

    /**
     * Reroutes the navigation to show the given view instead of the view that
     * is currently about to be displayed.
     *
     * <p>
     * Calling this method outside
     * {@link View#onLocationChange(LocationChangeEvent)} will not have any
     * effect.
     *
     * @param viewType
     *            the view type to display, not {@code null}
     */
    public void rerouteTo(Class<? extends View> viewType) {
        rerouteTo(new StaticViewRenderer(viewType, null));
    }

}
