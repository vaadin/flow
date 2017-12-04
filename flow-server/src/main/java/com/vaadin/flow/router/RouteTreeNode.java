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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.vaadin.router.NavigationHandler;

/**
 * A node in the routing tree that is built up based on the paths configured
 * using {@link RouterConfiguration#setRoute(String, NavigationHandler)}. Each
 * node in the tree corresponds to the options for a specific path.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class RouteTreeNode implements Serializable {
    /**
     * Special segment name used for identifying a placeholder child or route.
     */
    public static final String PLACEHOLDER_SEGMENT = "{}";

    private final Map<String, RouteTreeNode> children;

    private final Map<String, NavigationHandler> routes;

    private NavigationHandler wildcardHandler;

    /**
     * Creates a new empty node.
     */
    public RouteTreeNode() {
        children = new HashMap<>();
        routes = new HashMap<>();
    }

    /**
     * Creates a node as a copy of an existing node.
     *
     * @param original
     *            the node to copy, not <code>null</code>
     */
    public RouteTreeNode(RouteTreeNode original) {
        assert original != null;

        children = new HashMap<>(original.children);

        // Must clone the mutable node instances
        children.replaceAll((key, node) -> new RouteTreeNode(node));

        // Navigation handlers are considered immutable, so no cloning needed
        routes = new HashMap<>(original.routes);
        wildcardHandler = original.wildcardHandler;
    }

    private static <T> T getActualOrPlaceholder(Map<String, T> map,
            String key) {
        assert key != null;
        assert map != null;

        T value = map.get(key);
        if (value == null) {
            value = map.get(PLACEHOLDER_SEGMENT);
        }
        return value;
    }

    /**
     * Finds the child node for a given path segment. This first looks for a
     * child with the given name, and then falls back to the special
     * <code>{}</code> name for matching placeholders.
     *
     * @param segment
     *            the path segment name to look for, not <code>null</code>
     * @return a route tree node if one is found, otherwise <code>null</code>
     */
    public RouteTreeNode resolveChild(String segment) {
        return getActualOrPlaceholder(children, segment);
    }

    /**
     * Finds the navigation handler for a given path segment. This first looks
     * for a handler with the given name, and then falls back to the special
     * <code>{}</code> name for matching placeholders.
     *
     * @param segment
     *            the path segment name to look for, not <code>null</code>
     * @return a navigation handler if one is found, otherwise <code>null</code>
     */
    public NavigationHandler resolveRoute(String segment) {
        return getActualOrPlaceholder(routes, segment);
    }

    /**
     * Gets the wildcard handler that has been set for this node.
     *
     * @return the wildcard handler, or <code>null</code> if no such handler has
     *         been set
     */
    public NavigationHandler getWildcardHandler() {
        return wildcardHandler;
    }

    /**
     * Sets the wildcard handler to use for this node. The wildcard handler is
     * used if no more specific handler can be found for a location.
     *
     * @param wildcardHandler
     *            the wildcard handler to set, or <code>null</code> to remove
     *            the wildcard handler
     */
    public void setWildcardHandler(NavigationHandler wildcardHandler) {
        if (wildcardHandler != null && this.wildcardHandler != null) {
            throw new IllegalStateException(
                    "Wildcard route is already registered");
        }
        this.wildcardHandler = wildcardHandler;
    }

    /**
     * Set the navigation handler to use for the given path segment name.
     *
     * @param segment
     *            the path segment name, not <code>null</code>
     * @param navigationHandler
     *            the navigation handler to set, or <code>null</code> to remove
     *            the handler
     */
    public void setRoute(String segment, NavigationHandler navigationHandler) {
        assert segment != null;

        if (navigationHandler == null) {
            routes.remove(segment);
        } else {
            if (routes.containsKey(segment)) {
                throw new IllegalStateException("Route segment <" + segment
                        + "> is already registered");
            }
            routes.put(segment, navigationHandler);
        }
    }

    /**
     * Gets (or creates) a child node for the given path segment name.
     *
     * @param segment
     *            the path segment name, not <code>null</code>
     * @return the child node, not <code>null</code>
     */
    public RouteTreeNode getOrCreateChild(String segment) {
        assert segment != null;
        return children.computeIfAbsent(segment, k -> new RouteTreeNode());
    }

    /**
     * Checks if there is a child node for the given segment name.
     *
     * @param segment
     *            the path segment name, not <code>null</code>
     * @return <code>true</code> if there is a child, otherwise false
     */
    public boolean hasChild(String segment) {
        assert segment != null;
        return children.containsKey(segment);
    }

    /**
     * Removes the child node for the given path segment name.
     *
     * @param segment
     *            the path segment name, not <code>null</code>
     */
    public void removeChild(String segment) {
        assert segment != null;
        children.remove(segment);
    }

    /**
     * Checks whether this node is empty. An empty node has no child nodes, no
     * routes and no wildcard handler.
     *
     * @return <code>true</code> if the node is empty, <code>false</code>
     *         otherwise
     */
    public boolean isEmpty() {
        return children.isEmpty() && routes.isEmpty()
                && wildcardHandler == null;
    }

}
