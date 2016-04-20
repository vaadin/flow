/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.StateTree;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.HasText;

/**
 * A link that handles navigation internally using {@link Router} instead of
 * loading a new page in the browser.
 * <p>
 * The <code>href</code> attribute of {@link #getElement()} will only be
 * up-to-date when the component is attached to a UI.
 *
 * @since
 * @author Vaadin Ltd
 */
@Tag("a")
public class RouterLink extends Component implements HasText, HasComponents {

    private EventRegistrationHandle attachHandle;

    /**
     * Creates a new empty router link.
     */
    public RouterLink() {
        getElement().setAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE,
                "");
    }

    /**
     * Creates a new router link to the given view with the given text. The
     * provided parameters are used to populate placeholder and wildcard slots
     * in the route, starting from the beginning of the route. The number of
     * parameters must match the number of slots in the route.
     *
     * @param text
     *            the link text
     * @param viewType
     *            the view type to find a route for, not <code>null</code>
     * @param parameters
     *            the parameter values to set in the route
     */
    public RouterLink(String text, Class<? extends View> viewType,
            String... parameters) {
        this();
        setText(text);
        setRoute(viewType, parameters);
    }

    /**
     * Sets the target of this link. The provided parameters are used to
     * populate placeholder and wildcard slots in the route of the provided
     * view, starting from the beginning of the route. The number of parameters
     * must match the number of slots in the route. There must be exactly one
     * route configured for the view type.
     *
     * @param viewType
     *            the view type to find a route for, not <code>null</code>
     * @param parameters
     *            the parameter values to set in the route
     */
    public void setRoute(Class<? extends View> viewType, String... parameters) {
        if (getElement().getNode().isAttached()) {
            updateHref(viewType, parameters);
        } else {
            VaadinService currentService = VaadinService.getCurrent();
            if (currentService != null) {
                // Use the service to validate the mapping so that we can
                // provide early feedback if there's a problem.
                buildUrl(currentService.getRouter(), viewType, parameters);
            }

            if (attachHandle != null) {
                attachHandle.remove();
            }

            attachHandle = getElement().addAttachListener(e -> {
                attachHandle.remove();
                attachHandle = null;
                updateHref(viewType, parameters);
            });
        }
    }

    private void updateHref(Class<? extends View> viewType,
            String... parameters) {
        assert getElement().getNode().isAttached();

        StateTree tree = (StateTree) getElement().getNode().getOwner();

        Optional<Router> router = tree.getUI().getRouter();

        if (!router.isPresent()) {
            throw new IllegalArgumentException(
                    "RouterLink cannot be used if Router is not used");
        }

        String url = buildUrl(router.get(), viewType, parameters);

        getElement().setAttribute("href", url);
    }

    /**
     * Creates a URL for a view class by finding a route for it from a router
     * and populating placeholder and wildcard slots with the given parameter
     * values. The slots are populated starting from the beginning of the route.
     * The number of parameters must match the number of slots in the route.
     *
     * @param router
     *            the router to find view mappings from, not <code>null</code>
     * @param viewType
     *            the view type to find a route for, not <code>null</code>
     * @param parameters
     *            the parameter values to set in the route
     * @return a URL with all placeholder and wildcard slots populated
     * @throws IllegalArgumentException
     *             if there isn't exactly one route registered for the provided
     *             view type
     */
    public static String buildUrl(Router router, Class<? extends View> viewType,
            String... parameters) {
        assert router != null;
        assert viewType != null;

        // Throws IAE if there are multiple routes
        Optional<String> route = router.getConfiguration().getRoute(viewType);

        return route.map(r -> buildUrl(r, parameters))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No route defined for " + viewType.getName()));
    }

    /**
     * Creates a URL for a route by populating placeholder and wildcard slots
     * with the given parameter values. The slots are populated starting from
     * the beginning of the route. The number of parameters must match the
     * number of slots in the route.
     *
     * @param route
     *            the route to use, not <code>null</code>
     * @param parameters
     *            the parameter values to set in the route
     * @return a URL with all placeholder and wildcard slots populated
     */
    public static String buildUrl(String route, String... parameters) {
        assert route != null;
        assert parameters != null;

        int paramPos = 0;
        RouteLocation location = new RouteLocation(new Location(route));
        List<String> urlSegments = new ArrayList<>(
                location.getSegments().size());

        while (true) {
            if (location.startsWithPlaceholder()
                    || location.startsWithWildcard()) {
                if (paramPos >= parameters.length) {
                    throw new IllegalArgumentException(
                            route + " has more placeholders than the number of given parameters: "
                                    + Arrays.toString(parameters));
                }
                String parameter = parameters[paramPos++];
                assert parameter != null;
                urlSegments.add(parameter);
            } else {
                urlSegments.add(location.getFirstSegment());
            }

            Optional<RouteLocation> maybeSubLocation = location
                    .getRouteSubLocation();
            if (maybeSubLocation.isPresent()) {
                location = maybeSubLocation.get();
            } else {
                break;
            }
        }

        if (paramPos != parameters.length) {
            throw new IllegalArgumentException(
                    route + " has fewer placeholders than the number of given parameters: "
                            + Arrays.toString(parameters));
        }

        return new Location(urlSegments).getPath();
    }

}
