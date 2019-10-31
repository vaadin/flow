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

import java.util.Objects;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.StateTree;
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
 * @since 1.0
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
        getElement()
                .setAttribute(ApplicationConstants.ROUTER_LINK_ATTRIBUTE, "");
    }

    /**
     * Creates a new router link for the given navigation target using the given
     * text.
     *
     * @param text
     *         link text
     * @param navigationTarget
     *         navigation target
     */
    public RouterLink(String text,
            Class<? extends Component> navigationTarget) {
        this();
        setText(text);
        setRoute(getRouter(), navigationTarget);
    }

    /**
     * Creates a new router link for the given navigation target using the given
     * text and parameter.
     *
     * @param text
     *         link text
     * @param navigationTarget
     *         navigation target
     * @param parameter
     *         url parameter for navigation target
     * @param <T>
     *         url parameter type
     * @param <C>
     *         navigation target type
     */
    public <T, C extends Component & HasUrlParameter<T>> RouterLink(String text,
            Class<? extends C> navigationTarget, T parameter) {
        this();
        setText(text);
        setRoute(getRouter(), navigationTarget, parameter);
    }

    /**
     * Creates a new router link for the given navigation target using the given
     * text.
     *
     * @param router
     *         router used for navigation
     * @param text
     *         link text
     * @param navigationTarget
     *         navigation target
     */
    public RouterLink(Router router, String text,
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
     *         router used for navigation
     * @param text
     *         link text
     * @param navigationTarget
     *         navigation target
     * @param parameter
     *         url parameter for navigation target
     * @param <T>
     *         url parameter type
     * @param <C>
     *         navigation target type
     */
    public <T, C extends Component & HasUrlParameter<T>> RouterLink(
            Router router, String text, Class<? extends C> navigationTarget,
            T parameter) {
        this();
        setText(text);
        setRoute(router, navigationTarget, parameter);
    }

    /**
     * Set the navigation target for this link.
     *
     * @param router
     *         router used for navigation
     * @param navigationTarget
     *         navigation target
     */
    public void setRoute(Router router,
            Class<? extends Component> navigationTarget) {
        validateRouteParameters(router, navigationTarget);
        String url = RouteConfiguration.forRegistry(router.getRegistry())
                .getUrl(navigationTarget);
        updateHref(url);
    }

    /**
     * Set the navigation target for this link.
     *
     * @param router
     *         router used for navigation
     * @param navigationTarget
     *         navigation target
     * @param parameter
     *         url parameter for navigation target
     * @param <T>
     *         url parameter type
     * @param <C>
     *         navigation target type
     */
    public <T, C extends Component & HasUrlParameter<T>> void setRoute(
            Router router, Class<? extends C> navigationTarget, T parameter) {
        validateRouteParameters(router, navigationTarget);
        String url = RouteConfiguration.forRegistry(router.getRegistry())
                .getUrl(navigationTarget, parameter);
        updateHref(url);
    }

    /**
     * Set the navigation target for this link.
     *
     * @param navigationTarget
     *         navigation target
     */
    public void setRoute(Class<? extends Component> navigationTarget) {
        validateRouteParameters(getRouter(), navigationTarget);
        String url = RouteConfiguration.forRegistry(getRouter().getRegistry())
                .getUrl(navigationTarget);
        updateHref(url);
    }

    /**
     * Set the navigation target for this link.
     *
     * @param navigationTarget
     *         navigation target
     * @param parameter
     *         url parameter for navigation target
     * @param <T>
     *         url parameter type
     * @param <C>
     *         navigation target type
     */
    public <T, C extends Component & HasUrlParameter<T>> void setRoute(
            Class<? extends C> navigationTarget, T parameter) {
        validateRouteParameters(getRouter(), navigationTarget);
        String url = RouteConfiguration.forRegistry(getRouter().getRegistry())
                .getUrl(navigationTarget, parameter);
        updateHref(url);
    }

    private void validateRouteParameters(Router router,
            Class<? extends Component> navigationTarget) {
        if (router == null) {
            throw new IllegalArgumentException("Router must not be null");
        } else if (!router.getRegistry().getTargetUrl(navigationTarget)
                .isPresent()) {
            LoggerFactory.getLogger(RouterLink.class)
                    .warn("Creating link for non registered navigationTarget '"
                            + navigationTarget.getName()
                            + "'. If target is not registered when link is used, navigation will fail on a 404 not found.");
        }
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
     * @return an optional of {@link QueryParameters}, or an empty optional if
     * there are no query parameters set
     * @see #setQueryParameters(QueryParameters)
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
     *         the query parameters object, or {@code null} to remove
     *         existing query parameters
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

    private Router getRouter() {
        Router router = null;
        if (getElement().getNode().isAttached()) {
            StateTree tree = (StateTree) getElement().getNode().getOwner();
            router = tree.getUI().getRouter();
        }
        if (router == null) {
            router = VaadinService.getCurrent().getRouter();
        }
        if (router == null) {
            throw new IllegalStateException(
                    "Implicit router instance is not available. "
                            + "Use overloaded method with explicit router parameter.");
        }
        return router;
    }

    /**
     * Gets the {@link HighlightCondition} of this link.
     * <p>
     * The default condition is to checked whether the current location starts
     * with this link's {@link #getHref()} value, as defined in
     * {@link HighlightConditions#locationPrefix()}.
     *
     * @return the highlight condition, never {@code null}
     * @see #setHighlightCondition(HighlightCondition)
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
     * @param highlightCondition
     *         the highlight condition, not {@code null}
     * @see #setHighlightAction(HighlightAction)
     * @see HighlightConditions
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
     * @return the highlight action, never {@code null}
     * @see #setHighlightAction(HighlightAction)
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
     * @param highlightAction
     *         the highlight action, not {@code null}
     * @see #setHighlightCondition(HighlightCondition)
     * @see HighlightActions
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
