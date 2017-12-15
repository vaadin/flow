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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.ui.Page;

/**
 * A {@link Router} configuration object that may be in a modifiable state.
 * Since a configuration is used concurrently when handling requests, the
 * framework only provides access to an unsealed instance through the
 * {@link Router#reconfigure(RouterConfigurator)} method. This also means that
 * you should never need to create your own configuration instances.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class RouterConfiguration
        implements Serializable, ImmutableRouterConfiguration {
    private final boolean modifiable;

    /**
     * The root of the tree structure configured through
     * {@link #setRoute(String, NavigationHandler)}.
     * <p>
     * We should never give out references to this instance since it doesn't
     * have the any immutability checks.
     */
    private final RouteTreeNode routeTreeRoot;

    private final Map<Class<? extends View>, Class<? extends HasChildView>> parentViewTypes;

    private Resolver resolver;

    private Map<Class<? extends View>, List<String>> viewToRoute;

    private PageTitleGenerator pageTitleGenerator;

    private NavigationHandler errorHandler = new StaticViewRenderer(
            DefaultErrorView.class, null);

    /**
     * Creates a new empty immutable configuration.
     */
    public RouterConfiguration() {
        resolver = e -> Optional.empty();
        routeTreeRoot = new RouteTreeNode();
        parentViewTypes = new HashMap<>();
        viewToRoute = new HashMap<>();
        modifiable = false;
        pageTitleGenerator = new DefaultPageTitleGenerator();
    }

    /**
     * Creates a new configuration as a copy of the given configuration.
     *
     * @param original
     *            the original configuration to copy settings from, not
     *            <code>null</code>
     * @param modifiable
     *            <code>true</code> to set the instance as modifiable,
     *            <code>false</code> to set it as immutable
     */
    public RouterConfiguration(RouterConfiguration original,
            boolean modifiable) {
        assert original != null;

        resolver = original.resolver;

        pageTitleGenerator = original.pageTitleGenerator;

        errorHandler = original.errorHandler;

        routeTreeRoot = new RouteTreeNode(original.routeTreeRoot);

        parentViewTypes = new HashMap<>(original.parentViewTypes);

        viewToRoute = new HashMap<>();
        original.viewToRoute.forEach((viewType, routes) -> {
            viewToRoute.put(viewType, new ArrayList<>(routes));
        });

        this.modifiable = modifiable;
    }

    /**
     * Sets the resolver to use for resolving what to show for a given
     * navigation event. If the resolver doesn't provide a result, the routes
     * configured using the various <code>setRoute</code> methods will be
     * considered instead.
     *
     * @param resolver
     *            the resolver, not <code>null</code>
     */
    public void setResolver(Resolver resolver) {
        throwIfImmutable();
        if (resolver == null) {
            throw new IllegalArgumentException("Resolver cannot be null");
        }
        this.resolver = resolver;
    }

    private void throwIfImmutable() {
        if (!isModifiable()) {
            throw new IllegalStateException("Configuration is immutable");
        }
    }

    @Override
    public Resolver getResolver() {
        return resolver;
    }

    @Override
    public boolean isModifiable() {
        return modifiable;
    }

    /**
     * Resolves a route based on what has been configured using the various
     * <code>setRoute</code> methods.
     * <p>
     * If no route can be resolved and an error view has been set with
     * {@link #setErrorView(Class)}, then returns a handler for showing the
     * error view.
     *
     * @param location
     *            the location to resolve, not <code>null</code>
     * @return an optional navigation handler for handling the route, or empty
     *         optional if no configured route matched the location
     */
    @Override
    public Optional<NavigationHandler> resolveRoute(Location location) {
        assert location != null;

        // Start the recursion
        return Optional.ofNullable(resolveRoute(routeTreeRoot, location));
    }

    private static NavigationHandler resolveRoute(RouteTreeNode node,
            Location location) {
        String segment = location.getFirstSegment();
        Optional<Location> maybeSubLocation = location.getSubLocation();

        NavigationHandler handler = null;

        if (maybeSubLocation.isPresent()) {
            // Try to use a child node if there are more path segments
            RouteTreeNode childNode = node.resolveChild(segment);

            if (childNode != null) {
                handler = resolveRoute(childNode, maybeSubLocation.get());
            }
        } else {
            // Find an actual handler if this is the last path segment
            handler = node.resolveRoute(segment);
        }

        if (handler == null) {
            // Use a wildcard handler if we haven't found anything else
            handler = node.getWildcardHandler();
        }

        return handler;
    }

    /**
     * Set the view type and parent view type to use for the given path. The
     * parent view type may be rendered inside its own parent view type based on
     * configuration set using {@link #setParentView(Class, Class)}.
     * <p>
     * NOTE: A view can have one parent or none. If the given view has been
     * routed before without a parent, it will from now on always use the parent
     * set in this method.
     * <p>
     * The path is made up of segments separated by <code>/</code>. A segment
     * name enclosed in <code>{</code> and <code>}</code> is interpreted as a
     * placeholder segment. If no exact match is found when resolving a URL but
     * a placeholder is defined, then that placeholder will be used and the
     * actual path segment name will be available in the navigation event. The
     * last segment in a path can also be a wildcard, defined as <code>*</code>.
     * A wildcard route accepts any path segment names for the wildcard part,
     * but it will only be used if no route can be found using exact names or
     * placeholders.
     *
     * @param path
     *            the path for which the view type should be used, not
     *            <code>null</code>
     * @param viewType
     *            the view type to use for the path, not <code>null</code>
     * @param parentViewType
     *            the type of the parent view to use for the path, not
     *            <code>null</code>
     */
    public void setRoute(String path, Class<? extends View> viewType,
            Class<? extends HasChildView> parentViewType) {
        assert path != null;
        assert viewType != null;
        assert parentViewType != null;

        setParentView(viewType, parentViewType);
        setRoute(path, viewType);
    }

    private void mapViewToRoute(Class<? extends View> viewType, String path) {
        viewToRoute.computeIfAbsent(viewType, t -> new ArrayList<>()).add(path);
    }

    private void removeViewToRouteMapping(String route) {
        viewToRoute.values().forEach(routes -> routes.remove(route));
    }

    /**
     * Set the view type to use for the given path. The view type may be
     * rendered inside a parent view type based on configuration set using
     * {@link #setParentView(Class, Class)}.
     * <p>
     * NOTE: A view can have one parent or none. If this view has been
     * registered previously with a parent, the parent will be used for this
     * route too.
     * <p>
     * See {@link #setRoute(String, Class, Class)} for a description of the
     * supported path formats.
     *
     * @param path
     *            the path for which the view type should be used, not
     *            <code>null</code>
     * @param viewType
     *            the view type to use for the path, not <code>null</code>
     */
    public void setRoute(String path, Class<? extends View> viewType) {
        assert path != null;
        assert viewType != null;

        mapViewToRoute(viewType, path);
        setRoute(path, new StaticViewRenderer(viewType, path));
    }

    /**
     * Sets the parent view type for the given view type. For routes configured
     * using {@link #setRoute(String, Class)} or
     * {@link #setRoute(String, Class, Class)}, the parent view type will be
     * used whenever the child view type is rendered, so that the child view
     * type is always rendered inside the parent view type. This nesting is
     * applied recursively as long as a defined parent view type is found for
     * the previous parent type.
     * <p>
     * To change a mapping, you must explicitly remove it using
     * <code>setParentView(type, null)</code> first.
     *
     * @param viewType
     *            the type of the view for which to set a parent, not
     *            <code>null</code>
     * @param parentView
     *            the parent view type to use, or <code>null</code> to remove a
     *            mapping
     */
    public void setParentView(Class<? extends View> viewType,
            Class<? extends HasChildView> parentView) {
        assert viewType != null;

        if (parentView == null) {
            parentViewTypes.remove(viewType);
        } else if (!parentViewTypes.containsKey(viewType)) {
            if (getParentViewsAsList(parentView).contains(viewType)) {
                throw new IllegalStateException(
                        "Setting " + parentView + " as a parent of " + viewType
                                + " would create a loop");
            }

            parentViewTypes.put(viewType, parentView);
        } else if (parentView != parentViewTypes.get(viewType)) {
            throw new IllegalStateException(
                    "There is already a parent view configured for "
                            + viewType);
        }
    }

    @Override
    public Optional<Class<? extends HasChildView>> getParentView(
            Class<? extends View> viewType) {
        assert viewType != null;

        return Optional.ofNullable(parentViewTypes.get(viewType));
    }

    @Override
    public Stream<Class<? extends HasChildView>> getParentViewsAscending(
            Class<? extends View> viewType) {
        return getParentViewsAsList(viewType).stream();
    }

    /**
     * Gets the parent types configured for the given view type.
     * <p>
     * The returned list includes the parent view as returned by
     * {@link #getParentView(Class)} and recursively up until a view which does
     * not have a parent view.
     *
     * @param viewType
     *            the view type for which to find the parent views, not
     *            <code>null</code>
     * @return a list of parent view types
     */
    protected List<Class<? extends HasChildView>> getParentViewsAsList(
            Class<? extends View> viewType) {
        assert viewType != null;
        List<Class<? extends HasChildView>> parentViews = new ArrayList<>();
        Class<? extends View> currentType = viewType;
        while (true) {
            Class<? extends HasChildView> parentType = parentViewTypes
                    .get(currentType);
            if (parentType == null) {
                break;
            }
            parentViews.add(parentType);
            currentType = parentType;
        }
        return parentViews;
    }

    /**
     * Sets the navigation handler to use for the given path.
     * <p>
     * See {@link #setRoute(String, Class, Class)} for a description of the
     * supported path formats.
     *
     * @param path
     *            the path to use for the route
     * @param navigationHandler
     *            the navigation handler to use for the route
     */
    public void setRoute(String path, NavigationHandler navigationHandler) {
        assert path != null;
        assert navigationHandler != null;
        assert !path.startsWith("/");
        assert !path.contains("://");

        // Start the recursion
        setRoute(new RouteLocation(new Location(path)), routeTreeRoot,
                navigationHandler);
    }

    private void setRoute(RouteLocation location, RouteTreeNode node,
            NavigationHandler navigationHandler) {
        throwIfImmutable();

        Optional<RouteLocation> maybeSubLocation = location
                .getRouteSubLocation();

        if (location.startsWithWildcard()) {
            if (maybeSubLocation.isPresent()) {
                throw new IllegalArgumentException(
                        "Wildcard should be last segment");
            }
            node.setWildcardHandler(navigationHandler);
            return;
        }

        String segment = getFirstSegmentOrPlaceholderToken(location);

        if (maybeSubLocation.isPresent()) {
            // Configure the rest of the location in a child node
            RouteTreeNode childNode = node.getOrCreateChild(segment);

            setRoute(maybeSubLocation.get(), childNode, navigationHandler);
        } else {
            // Record the navigation handler for this final part of the location
            node.setRoute(segment, navigationHandler);
        }
    }

    /**
     * Removes the route described by the given path. See
     * {@link #setRoute(String, NavigationHandler)} for a description of the
     * supported path formats.
     *
     * @param path
     *            the path of the route to remove, not <code>null</code>
     */
    public void removeRoute(String path) {
        assert path != null;

        removeViewToRouteMapping(path);
        // Start the recursion
        removeRoute(new RouteLocation(new Location(path)), routeTreeRoot);
    }

    private void removeRoute(RouteLocation location, RouteTreeNode node) {
        throwIfImmutable();

        if (location.startsWithWildcard()) {
            node.setWildcardHandler(null);
            return;
        }

        String segment = getFirstSegmentOrPlaceholderToken(location);

        Optional<RouteLocation> maybeSubLocation = location
                .getRouteSubLocation();

        if (!maybeSubLocation.isPresent()) {
            node.setRoute(segment, null);
        } else if (node.hasChild(segment)) {
            RouteTreeNode childNode = node.getOrCreateChild(segment);

            removeRoute(maybeSubLocation.get(), childNode);
            if (childNode.isEmpty()) {
                node.removeChild(segment);
            }
        }
    }

    private static String getFirstSegmentOrPlaceholderToken(
            RouteLocation location) {
        if (location.startsWithPlaceholder()) {
            return RouteTreeNode.PLACEHOLDER_SEGMENT;
        } else {
            return location.getFirstSegment();
        }
    }

    @Override
    public Stream<String> getRoutes(Class<? extends View> viewType) {
        return viewToRoute.getOrDefault(viewType, Collections.emptyList())
                .stream();
    }

    @Override
    public Optional<String> getRoute(Class<? extends View> viewType)
            throws IllegalArgumentException {
        Stream<String> routes = getRoutes(viewType);
        List<String> l = routes.collect(Collectors.toList());
        if (l.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple routes are defined for the given view type "
                            + viewType.getName());
        } else {
            return l.stream().findFirst();
        }

    }

    @Override
    public PageTitleGenerator getPageTitleGenerator() {
        return pageTitleGenerator;
    }

    /**
     * Sets the {@link PageTitleGenerator} to use for generating the
     * {@link Page#setTitle(String) page title} according to the navigation.
     * <p>
     * The default is the {@link DefaultPageTitleGenerator}.
     * <p>
     * Setting <code>null</code> is not allowed.
     *
     * @param pageTitleGenerator
     *            the page title generator to use
     */
    public void setPageTitleGenerator(PageTitleGenerator pageTitleGenerator) {
        throwIfImmutable();

        if (pageTitleGenerator == null) {
            throw new IllegalArgumentException(
                    "PageTitleGenerator cannot be null");
        }

        this.pageTitleGenerator = pageTitleGenerator;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public NavigationHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the error handler to use.
     * <p>
     * The error handler shows views corresponding to the 404 error page. It is
     * used when the user tries to navigate into an undefined route.
     * <p>
     * The default error handler uses a {@link StaticViewRenderer} to show
     * {@link DefaultErrorView}.
     *
     * @param errorHandler
     *            the navigation handler to use for showing errors, not
     *            <code>null</code>
     */
    public void setErrorHandler(NavigationHandler errorHandler) {
        if (errorHandler == null) {
            throw new IllegalArgumentException("errorHandler cannot be null");
        }
        throwIfImmutable();
        this.errorHandler = errorHandler;
    }

    /**
     * Sets the error view type to use. This is a shorthand for calling
     * {@link #setErrorHandler(NavigationHandler)} with a
     * {@link StaticViewRenderer}.
     * <p>
     * The error view corresponds to the 404 error page. It is shown when the
     * user tries to navigate into an undefined route.
     * <p>
     * The default error view is {@link DefaultErrorView}.
     * <p>
     * To specify a parent view for the error view, use
     * {@link #setErrorView(Class, Class)} instead of this method or use
     * {@link #setParentView(Class, Class)} after calling this method.
     * <p>
     * NOTE: if this view type has been registered for a route previously with a
     * parent view, then the same parent view will be shown when this view is
     * opened as an error view.
     *
     * @param errorView
     *            the error view type to shown, not <code>null</code>
     */
    public void setErrorView(Class<? extends View> errorView) {
        if (errorView == null) {
            throw new IllegalArgumentException("errorView cannot be null");
        }

        setErrorHandler(new StaticViewRenderer(errorView, null) {
            @Override
            public int handle(NavigationEvent event) {
                int statusCode = super.handle(event);

                // Override status code for the error view if the view itself
                // hasn't set any specific code
                if (statusCode == HttpServletResponse.SC_OK) {
                    statusCode = 404;
                }

                return statusCode;
            }
        });
    }

    /**
     * Sets the error view type to use with the given parent view type.
     * <p>
     * This method is shorthand for calling {@link #setErrorView(Class)} and
     * {@link #setParentView(Class, Class)}.
     * <p>
     * To specify an error view without a parent view, use
     * {@link #setErrorView(Class)}.
     *
     * @param errorView
     *            the error view type to shown, not <code>null</code>
     * @param parentView
     *            the type of the parent view for the error view, not
     *            <code>null</code>
     * @see #setErrorView(Class)
     */
    public void setErrorView(Class<? extends View> errorView,
            Class<? extends HasChildView> parentView) {
        if (parentView == null) {
            throw new IllegalArgumentException("parentView cannot be null");
        }
        setErrorView(errorView);
        setParentView(errorView, parentView);
    }
}
