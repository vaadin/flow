package com.vaadin.flow.server;

import com.vaadin.flow.shared.VaadinUriResolver;

/**
 * A URI resolver which resolves paths for loading through VaadinService
 * resource methods.
 */
public class ServiceContextUriResolver extends VaadinUriResolver {

    /**
     * Resolves the given uri using the given frontend location, to a path which
     * can be used with
     * {@link VaadinService#getResource(String, WebBrowser, com.vaadin.flow.theme.AbstractTheme)}
     * and
     * {@link VaadinService#getResourceAsStream(String, WebBrowser, com.vaadin.flow.theme.AbstractTheme)}..
     *
     * @param uri
     *            the URI to resolve
     * @param frontendUrl
     *            the location of the <code>frontend</code> folder
     * @return the URI resolved to be relative to the context root
     */
    public String resolveVaadinUri(String uri, String frontendUrl) {
        return super.resolveVaadinUri(uri, frontendUrl, "/");
    }

}
