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
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.router.legacy.RouteLocation;
import com.vaadin.flow.router.legacy.RouteLocation.RouteSegmentVisitor;
import com.vaadin.flow.router.legacy.Router;
import com.vaadin.flow.router.legacy.View;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * A link that handles navigation internally using {@link Router} instead of
 * loading a new page in the browser.
 * <p>
 * The <code>href</code> attribute of {@link #getElement()} will only be
 * up-to-date when the component is attached to a UI.
 *
 * @author Vaadin Ltd
 */
@Tag(Tag.A)
public class RouterLink extends Component
        implements HasText, HasComponents, HasStyle, AfterNavigationObserver {

    private static final PropertyDescriptor<String, String> HREF = PropertyDescriptors
            .attributeWithDefault("href", "", false);

    private HighlightCondition<RouterLink> highlightCondition = HighlightConditions
            .locationPrefix();

    private HighlightAction<RouterLink> highlightAction = HighlightActions
            .toggleAttribute("highlight");

    private QueryParameters queryParameters;

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
     * Creates a new router link for the given navigation target using the given
     * text.
     *
     * @param text
     *            link text
     * @param navigationTarget
     *            navigation target
     */
    public RouterLink(String text,
            Class<? extends Component> navigationTarget) {
        this();
        setText(text);
        if (View.class.isAssignableFrom(navigationTarget)) {
            try {
                setRoute((Router) getRouter(),
                        (Class<? extends View>) navigationTarget);
            } catch (ClassCastException cce) {
                String message = String.format(
                        "Only navigation targets for old Router should implement 'View'. Remove 'implements View' from '%s'",
                        navigationTarget.getName());
                throw new IllegalArgumentException(message, cce);
            }
        } else {
            setRoute((com.vaadin.flow.router.Router) getRouter(),
                    navigationTarget);
        }
    }

    /**
     * Creates a new router link for the given navigation target using the given
     * text and parameter.
     *
     * @param text
     *            link text
     * @param navigationTarget
     *            navigation target
     * @param parameter
     *            url parameter for navigation target
     * @param <T>
     *            url parameter type
     * @param <C>
     *            navigation target type
     */
    public <T, C extends Component & HasUrlParameter<T>> RouterLink(String text,
            Class<? extends C> navigationTarget, T parameter) {
        this();
        setText(text);
        setRoute((com.vaadin.flow.router.Router) getRouter(), navigationTarget,
                parameter);
    }

    /**
     * Creates a new router link for the given navigation target using the given
     * text.
     *
     * @param router
     *            router used for navigation
     * @param text
     *            link text
     * @param navigationTarget
     *            navigation target
     */
    public RouterLink(com.vaadin.flow.router.Router router, String text,
            Class<? extends Component> navigationTarget) {
        this();
        setText(text);
        setRoute(router, navigationTarget);
    }

    /**
     * Creates a new router link for the given navigation target using the given
     * text and parameter.
     *
     * @param router
     *            router used for navigation
     * @param text
     *            link text
     * @param navigationTarget
     *            navigation target
     * @param parameter
     *            url parameter for navigation target
     * @param <T>
     *            url parameter type
     * @param <C>
     *            navigation target type
     */
    public <T, C extends Component & HasUrlParameter<T>> RouterLink(
            com.vaadin.flow.router.Router router, String text,
            Class<? extends C> navigationTarget, T parameter) {
        this();
        setText(text);
        setRoute(router, navigationTarget, parameter);
    }

    /**
     * Set the navigation target for this link.
     *
     * @param router
     *            router used for navigation
     * @param navigationTarget
     *            navigation target
     */
    public void setRoute(com.vaadin.flow.router.Router router,
            Class<? extends Component> navigationTarget) {
        validateRouteParameters(router, navigationTarget);
        String url = router.getUrl(navigationTarget);
        updateHref(url);
    }

    /**
     * Set the navigation target for this link.
     *
     * @param router
     *            router used for navigation
     * @param navigationTarget
     *            navigation target
     * @param parameter
     *            url parameter for navigation target
     * @param <T>
     *            url parameter type
     * @param <C>
     *            navigation target type
     */
    public <T, C extends Component & HasUrlParameter<T>> void setRoute(
            com.vaadin.flow.router.Router router,
            Class<? extends C> navigationTarget, T parameter) {
        validateRouteParameters(router, navigationTarget);
        String url = router.getUrl(navigationTarget, parameter);
        updateHref(url);
    }

    private void validateRouteParameters(com.vaadin.flow.router.Router router,
            Class<?> navigationTarget) {
        if (router == null) {
            throw new IllegalArgumentException("Router must not be null");
        } else if (!navigationTarget.isAnnotationPresent(Route.class)) {
            throw new IllegalArgumentException(
                    "Given navigation target is not an @Route target!");
        }
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
        RouterInterface router = getRouter();
        updateHref((Router) router, viewType, parameters);
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
     * Gets the {@link QueryParameters} of this link.
     *
     * @see #setQueryParameters(QueryParameters)
     *
     * @return an optional of {@link QueryParameters}, or an empty optional if
     *         there are no query parameters set
     */
    public Optional<QueryParameters> getQueryParameters() {
        return Optional.ofNullable(queryParameters);
    }

    /**
     * Sets the {@link QueryParameters} of this link.
     * <p>
     * The query string will be generated from
     * {@link QueryParameters#getQueryString()} and will be appended to the
     * {@code href} attribute of this link.
     *
     * @param queryParameters
     *            the query parameters object, or {@code null} to remove
     *            existing query parameters
     */
    public void setQueryParameters(QueryParameters queryParameters) {
        this.queryParameters = queryParameters;
        updateHref(getHref());
    }

    private void updateHref(String url) {
        int startOfQuery = url.indexOf('?');
        if (startOfQuery >= 0) {
            url = url.substring(0, startOfQuery);
        }
        if (queryParameters != null) {
            url += '?' + queryParameters.getQueryString();
        }
        HREF.set(this, url);
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
                    throw new IllegalArgumentException(route
                            + " has more placeholders than the number of given parameters: "
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
            throw new IllegalArgumentException(route
                    + " has fewer placeholders than the number of given parameters: "
                    + Arrays.toString(parameters));
        }

        return new Location(urlSegments).getPath();
    }

    private RouterInterface getRouter() {
        Optional<RouterInterface> router = Optional.empty();
        if (getElement().getNode().isAttached()) {
            StateTree tree = (StateTree) getElement().getNode().getOwner();
            router = tree.getUI().getRouterInterface();
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
        return router.get();
    }

    /**
     * Gets the {@link HighlightCondition} of this link.
     * <p>
     * The default condition is to checked whether the current location starts
     * with this link's {@link #getHref()} value, as defined in
     * {@link HighlightConditions#locationPrefix()}.
     *
     * @see #setHighlightCondition(HighlightCondition)
     *
     * @return the highlight condition, never {@code null}
     */
    public HighlightCondition<RouterLink> getHighlightCondition() {
        return highlightCondition;
    }

    /**
     * Sets the {@link HighlightCondition} of this link, which determines if the
     * link should be highlighted when a {@link AfterNavigationEvent} occurs.
     * <p>
     * The evaluation of this condition will be processed by this link's
     * {@link HighlightAction}.
     *
     * @see #setHighlightAction(HighlightAction)
     * @see HighlightConditions
     *
     * @param highlightCondition
     *            the highlight condition, not {@code null}
     */
    public void setHighlightCondition(
            HighlightCondition<RouterLink> highlightCondition) {
        Objects.requireNonNull(highlightCondition,
                "HighlightCondition may not be null");

        this.highlightCondition = highlightCondition;
    }

    /**
     * Gets the {@link HighlightAction} of this link.
     * <p>
     * The default action is to toggle the {@code highlight} attribute of the
     * element, as defined in {@link HighlightActions#toggleAttribute(String)}.
     *
     * @see #setHighlightAction(HighlightAction)
     *
     * @return the highlight action, never {@code null}
     */
    public HighlightAction<RouterLink> getHighlightAction() {
        return highlightAction;
    }

    /**
     * Sets the {@link HighlightAction} of this link, which will be performed
     * with the evaluation of this link's {@link HighlightCondition}.
     * <p>
     * The old action will be executed passing {@code false} to
     * {@link HighlightAction#highlight(Object, boolean)} to clear any previous
     * highlight state.
     *
     * @see #setHighlightCondition(HighlightCondition)
     * @see HighlightActions
     *
     * @param highlightAction
     *            the highlight action, not {@code null}
     */
    public void setHighlightAction(
            HighlightAction<RouterLink> highlightAction) {
        Objects.requireNonNull(highlightCondition,
                "HighlightAction may not be null");

        this.highlightAction.highlight(this, false);
        this.highlightAction = highlightAction;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getHighlightAction().highlight(this,
                getHighlightCondition().shouldHighlight(this, event));
    }
}
