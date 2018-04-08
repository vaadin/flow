package com.vaadin.flow.server;

import com.vaadin.flow.shared.VaadinUriResolver;

/**
 * A URI resolver which resolves paths for loading through the servlet context.
 */
public class ServletContextUriResolver extends VaadinUriResolver {

    /**
     * Resolves the given uri using the given frontend location.
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
