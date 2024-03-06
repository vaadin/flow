/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.shared.ui.Dependency;

/**
 * Filter for dependencies loaded using {@link StyleSheet @StyleSheet}, and *
 * {@link JavaScript @JavaScript}.
 *
 * @see ServiceInitEvent#addDependencyFilter(DependencyFilter)
 * @since 1.0
 */
@FunctionalInterface
public interface DependencyFilter extends Serializable {

    /**
     * Filters the list of dependencies and returns a (possibly) updated
     * version.
     * <p>
     * Called whenever dependencies are about to be sent to the client side for
     * loading and when templates are parsed on the server side.
     *
     * @param dependencies
     *            the collected dependencies, possibly already modified by other
     *            filters
     * @param service
     *            a Vaadin service
     * @return a list of dependencies to load
     */
    List<Dependency> filter(List<Dependency> dependencies,
            VaadinService service);

}
