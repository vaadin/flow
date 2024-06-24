/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.shared.ui.Dependency;

/**
 * Filter for dependencies loaded using {@link StyleSheet @StyleSheet},
 * {@link JavaScript @JavaScript} and {@link HtmlImport @HtmlImport}.
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
     * @param filterContext
     *            context information, e.g about the target UI
     * @return a list of dependencies to load
     */
    List<Dependency> filter(List<Dependency> dependencies,
            FilterContext filterContext);

    /**
     * Provides context information for the dependency filter operation.
     */
    public static class FilterContext implements Serializable {

        private final VaadinService service;
        private final WebBrowser browser;

        /**
         * Creates a new context for the given session.
         *
         * @param service
         *            the service which is loading dependencies
         * @param browser
         *            the browser
         */
        public FilterContext(VaadinService service, WebBrowser browser) {
            this.service = service;
            this.browser = browser;
        }

        /**
         * Gets the related Vaadin service.
         *
         * @return the Vaadin service
         */
        public VaadinService getService() {
            return service;
        }

        /**
         * Gets the browser info needed for filtering.
         *
         * @return the WebBrowser used
         */
        public WebBrowser getBrowser() {
            return browser;
        }
    }

}
