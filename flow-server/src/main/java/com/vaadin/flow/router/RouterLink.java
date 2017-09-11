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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.vaadin.annotations.Tag;
import com.vaadin.flow.StateTree;
import com.vaadin.flow.router.RouteLocation.RouteSegmentVisitor;
import com.vaadin.server.VaadinService;
import com.vaadin.shared.ApplicationConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.HasStyle;
import com.vaadin.ui.HasText;
import com.vaadin.ui.PropertyDescriptor;
import com.vaadin.ui.PropertyDescriptors;
import com.vaadin.ui.UI;

/**
 * A link that handles navigation internally using {@link Router} instead of
 * loading a new page in the browser.
 * <p>
 * The <code>href</code> attribute of {@link #getElement()} will only be
 * up-to-date when the component is attached to a UI.
 *
 * @author Vaadin Ltd
 */
@Tag("a")
public class RouterLink extends Component
        implements HasText, HasComponents, HasStyle {

    private static final PropertyDescriptor<String, String> HREF = PropertyDescriptors
            .attributeWithDefault("href", "", false);

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
     * <p>
     * This is a shorthand for
     * {@link #RouterLink(Router, String, Class, String...)} which uses
     * implicitly available {@link Router} instance from the current {@link UI}.
     *
     * @see #RouterLink(Router, String, Class, String...)
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
     * Creates a new router link to the given view with the given text. The
     * provided parameters are used to populate placeholder and wildcard slots
     * in the route, starting from the beginning of the route. The number of
     * parameters must match the number of slots in the route.
     *
     * @param router
     *            the router used for navigating
     * @param text
     *            the link text
     * @param viewType
     *            the view type to find a route for, not <code>null</code>
     * @param parameters
     *            the parameter values to set in the route
     */
    public RouterLink(Router router, String text,
            Class<? extends View> viewType, String... parameters) {
        this();
        setText(text);
        setRoute(router, viewType, parameters);
    }

    /**
     * Sets the target of this link. The provided parameters are used to
     * populate placeholder and wildcard slots in the route of the provided
     * view, starting from the beginning of the route. The number of parameters
     * must match the number of slots in the route. There must be exactly one
     * route configured for the view type.
     *
     * @param router
     *            the router used for navigating, not {@code null}
     * @param viewType
     *            the view type to find a route for, not <code>null</code>
     * @param parameters
     *            the parameter values to set in the route
     */
    public void setRoute(Router router, Class<? extends View> viewType,
            String... parameters) {
        if (router == null) {
            throw new IllegalArgumentException("Router must not be null");
        }
        updateHref(router, viewType, parameters);
    }

    /**
     * Sets the target of this link. The provided parameters are used to
     * populate placeholder and wildcard slots in the route of the provided
     * view, starting from the beginning of the route. The number of parameters
     * must match the number of slots in the route. There must be exactly one
     * route configured for the view type.
     * <p>
     * This is a shorthand for {@link #setRoute(Router, Class, String...)}
     * method which uses implicitly available {@link Router} instance from the
     * current {@link UI}.
     *
     * @see #setRoute(Router, Class, String...)
     *
     * @param viewType
     *            the view type to find a route for, not <code>null</code>
     * @param parameters
     *            the parameter values to set in the route
     */
    public void setRoute(Class<? extends View> viewType, String... parameters) {
        Optional<RouterInterface> router = Optional.empty();
        if (getElement().getNode().isAttached()) {
            StateTree tree = (StateTree) getElement().getNode().getOwner();
            router = tree.getUI().getRouter();
            if (!router.isPresent()) {
                throw new IllegalArgumentException(
                        "RouterLink cannot be used if Router is not used");
            }
        }
        if (!router.isPresent()) {
            router = Optional.ofNullable(VaadinService.getCurrent())
                    .map(VaadinService::getRouter);
        }
        if (!router.isPresent()) {
            throw new IllegalStateException(
                    "Implicit router instance is not available. "
                            + "Use overloaded method with explicit router parameter.");
        }
        updateHref((Router) router.get(), viewType, parameters);
    }

    private void updateHref(Router router, Class<? extends View> viewType,
            String... parameters) {
        assert router != null;
        String url = buildUrl(router, viewType, parameters);

        HREF.set(this, url);
    }

    /**
     * Gets the href (the URL) of this link.
     *
     * @return the href
     */
    public String getHref() {
        return HREF.get(this);
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

        Iterator<String> parametersIterator = Arrays.asList(parameters)
                .iterator();
        RouteLocation location = new RouteLocation(new Location(route));
        List<String> urlSegments = new ArrayList<>(
                location.getSegments().size());

        location.visitSegments(new RouteSegmentVisitor() {
            @Override
            public void acceptPlaceholder(String placeholderName) {
                if (!parametersIterator.hasNext()) {
                    throw new IllegalArgumentException(
                            route + " has more placeholders than the number of given parameters: "
                                    + Arrays.toString(parameters));
                }
                urlSegments.add(parametersIterator.next());
            }

            @Override
            public void acceptWildcard() {
                if (parametersIterator.hasNext()) {
                    urlSegments.add(parametersIterator.next());
                } else {
                    urlSegments.add("");
                }
            }

            @Override
            public void acceptSegment(String segment) {
                urlSegments.add(segment);
            }
        });

        if (parametersIterator.hasNext()) {
            throw new IllegalArgumentException(
                    route + " has fewer placeholders than the number of given parameters: "
                            + Arrays.toString(parameters));
        }

        return new Location(urlSegments).getPath();
    }

}
