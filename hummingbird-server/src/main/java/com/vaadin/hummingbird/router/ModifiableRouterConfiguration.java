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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.annotations.Title;
import com.vaadin.ui.Page;

/**
 * A {@link Router} configuration object that may be in a modifiable state.
 * Since a configuration is used concurrently when handling requests, the
 * framework only provides access to an unsealed instance through the
 * {@link Router#reconfigure(RouterConfigurator)} method. This also means that
 * you should never need to create your own configuration instances.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ModifiableRouterConfiguration
        implements Serializable, RouterConfiguration {
    private final boolean modifiable;

    /**
     * The root of the tree structure configured through
     * {@link #setRoute(String, NavigationHandler)}.
     * <p>
     * We should never give out references to this instance since it doesn't
     * have the any immutability checks.
     */
    private final RouteTreeNode routeTreeRoot;

    private final HashMap<Class<? extends View>, Class<? extends HasChildView>> parentViewTypes;

    private Resolver resolver;

    private HashMap<Class<? extends View>, List<String>> viewToRoute;

    private PageTitleGenerator pageTitleGenerator;

    /**
     * Creates a new empty immutable configuration.
     */
    public ModifiableRouterConfiguration() {
        resolver = e -> null;
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
    public ModifiableRouterConfiguration(ModifiableRouterConfiguration original,
            boolean modifiable) {
        assert original != null;

        resolver = original.resolver;

        pageTitleGenerator = original.pageTitleGenerator;

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
     *
     * @param event
     *            the event for which to resolve a route
     * @return a navigation handler or handling the route, or <code>null</code>
     *         if no configured route matched the location
     */
    @Override
    public NavigationHandler resolveRoute(NavigationEvent event) {
        assert event != null;

        // Start the recursion
        return resolveRoute(routeTreeRoot, event.getLocation(), event);
    }

    private static NavigationHandler resolveRoute(RouteTreeNode node,
            Location location, NavigationEvent event) {
        String segment = location.getFirstSegment();
        Location subLocation = location.getSubLocation();

        NavigationHandler handler = null;

        if (subLocation.hasSegments()) {
            // Try to use a child node if there are more path segments
            RouteTreeNode childNode = node.resolveChild(segment);

            if (childNode != null) {
                handler = resolveRoute(childNode, subLocation, event);
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
        setRoute(path, new ViewRenderer() {
            @Override
            public Class<? extends View> getViewType() {
                return viewType;
            }

            @Override
            public List<Class<? extends HasChildView>> getParentViewTypes() {
                return getParentViewsAsList(viewType);
            }

            @Override
            protected String getRoute() {
                return path;
            }
        });
    }

    /**
     * Sets the parent view type for the given view type. For routes configured
     * using {@link #setRoute(String, Class)} or
     * {@link #setRoute(String, Class, Class)}, the parent view type will be
     * used whenever the child view type is rendered, so that the child view
     * type is rendered inside the parent view type. This nesting is applied
     * recursively as long as a defined parent view type is found for the
     * previous parent type.
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
        } else {
            throw new IllegalStateException(
                    "There is alerady a parent view configured for "
                            + viewType);
        }
    }

    @Override
    public Class<? extends HasChildView> getParentView(
            Class<? extends View> viewType) {
        assert viewType != null;

        return parentViewTypes.get(viewType);
    }

    @Override
    public Stream<Class<? extends HasChildView>> getParentViews(
            Class<? extends View> viewType) {
        return getParentViewsAsList(viewType).stream();
    }

    private ArrayList<Class<? extends HasChildView>> getParentViewsAsList(
            Class<? extends View> viewType) {
        ArrayList<Class<? extends HasChildView>> parentViews = new ArrayList<>();
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

        RouteLocation subLocation = location.getSubLocation();

        if (location.startsWithWildcard()) {
            if (subLocation.hasSegments()) {
                throw new IllegalArgumentException(
                        "Wildcard should be last segment");
            }
            node.setWildcardHandler(navigationHandler);
            return;
        }

        String segment = getFirstSegmentOrPlaceholderToken(location);

        if (subLocation.hasSegments()) {
            // Configure the rest of the location in a child node
            RouteTreeNode childNode = node.getOrCreateChild(segment);

            setRoute(subLocation, childNode, navigationHandler);
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

        RouteLocation subLocation = location.getSubLocation();

        if (!subLocation.hasSegments()) {
            node.setRoute(segment, null);
        } else if (node.hasChild(segment)) {
            RouteTreeNode childNode = node.getOrCreateChild(segment);

            removeRoute(subLocation, childNode);
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
    public Optional<PageTitleGenerator> getPageTitleGenerator() {
        return Optional.ofNullable(pageTitleGenerator);
    }

    /**
     * Sets the {@link PageTitleGenerator} to use for generating the
     * {@link Page#setTitle(String) page title} according to the navigation.
     * <p>
     * Overrides the default page title generation with
     * {@link View#getTitle(LocationChangeEvent)} or {@link Title @Title}.
     * <p>
     * By default this is <code>null</code>.
     * <p>
     * NOTE: it is the responsibility of the {@link NavigationHandler}s to
     * handle the updating of the title according to the navigation.
     *
     * @param pageTitleGenerator
     *            the page title generator to use
     * @see ViewRenderer#updatePageTitle(NavigationEvent, LocationChangeEvent)
     */
    public void setPageTitleGenerator(PageTitleGenerator pageTitleGenerator) {
        throwIfImmutable();

        this.pageTitleGenerator = pageTitleGenerator;
    }
}
