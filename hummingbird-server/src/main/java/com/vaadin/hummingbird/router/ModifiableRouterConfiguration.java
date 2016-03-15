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

    private Resolver resolver;

    /**
     * Creates a new empty immutable configuration.
     */
    public ModifiableRouterConfiguration() {
        resolver = e -> null;
        routeTreeRoot = new RouteTreeNode();
        modifiable = false;
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

        routeTreeRoot = new RouteTreeNode(original.routeTreeRoot);

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

    /**
     * Checks whether this configuration can be modified.
     *
     * @return <code>true</code> if it is modifiable, <code>false</code> if it
     *         immutable
     */
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
     * Set the view type and parent view types to use for the given path. See
     * {@link #setRoute(String, NavigationHandler)} for a description of the
     * supported path formats.
     *
     * @param path
     *            the path for which the view type should be used, not
     *            <code>null</code>
     * @param viewType
     *            the view type to use for the path, not <code>null</code>
     * @param parentViewTypes
     *            optional list of parent view types, not <code>null</code>
     */
    @SafeVarargs
    public final void setRoute(String path, Class<? extends View> viewType,
            Class<? extends HasChildView>... parentViewTypes) {
        assert path != null;
        assert viewType != null;
        assert parentViewTypes != null;

        setRoute(path, new ViewRenderer(viewType, parentViewTypes));
    }

    /**
     * Sets the navigation handler to use for the given path.
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
        setRoute(new Location(path), routeTreeRoot, navigationHandler);
    }

    private void setRoute(Location location, RouteTreeNode node,
            NavigationHandler navigationHandler) {
        throwIfImmutable();

        String segment = location.getFirstSegment();
        Location subLocation = location.getSubLocation();

        if (isWildcardSegment(segment)) {
            if (subLocation.hasSegments()) {
                throw new IllegalArgumentException(
                        "Wildcard should be last segment");
            }
            node.setWildcardHandler(navigationHandler);
            return;
        }

        if (isPlaceholderSegment(segment)) {
            // A future patch that makes the placeholder values available in the
            // event should record the used name here.
            segment = "{}";
        }

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
     * Helper for checking whether a segment is a wildcard segment. Also checks
     * for illegal use of the wildcard segment identifier if the segment is not
     * a wildcard.
     *
     * @param segment
     *            the segment to check, not <code>null</code>
     * @return <code>true</code> if the segment is a wildcard segment,
     *         <code>false</code> otherwise.
     */
    private static boolean isWildcardSegment(String segment) {
        assert segment != null;

        boolean isWildcard = "*".equals(segment);
        if (!isWildcard && segment.contains("*")) {
            throw new IllegalArgumentException("* is only valid as \"/*\"");

        }
        return isWildcard;
    }

    /**
     * Helper for checking whether a segment is a placeholder segment. Also
     * checks for illegal use of the placeholder segment identifiers if the
     * segment is not a placeholder.
     *
     * @param segment
     *            the segment to check, not <code>null</code>
     * @return <code>true</code> if the segment is a placeholder segment,
     *         <code>false</code> otherwise.
     */
    private static boolean isPlaceholderSegment(String segment) {
        boolean isPlaceholder = segment.startsWith("{")
                && segment.endsWith("}");
        if (!isPlaceholder
                && (segment.contains("{") || segment.contains("}"))) {
            throw new IllegalArgumentException(
                    "{ and } are only allowed in the start and end of a segment");
        }
        return isPlaceholder;
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

        // Start the recursion
        removeRoute(new Location(path), routeTreeRoot);
    }

    private void removeRoute(Location location, RouteTreeNode node) {
        throwIfImmutable();

        String segment = location.getFirstSegment();
        Location subLocation = location.getSubLocation();

        if (isWildcardSegment(segment)) {
            node.setWildcardHandler(null);
            return;
        }

        if (isPlaceholderSegment(segment)) {
            segment = "{}";
        }

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
}
