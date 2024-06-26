/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.shared.VaadinUriResolver;

/**
 * A URI resolver which resolves paths for loading through VaadinService
 * resource methods.
 *
 * @since 1.0
 */
public class ServiceContextUriResolver extends VaadinUriResolver
        implements Serializable {

    /**
     * Resolves the given uri using the given frontend location, to a path which
     * can be used with
     * {@link VaadinService#getResource(String, WebBrowser, com.vaadin.flow.theme.AbstractTheme)}
     * and
     * {@link VaadinService#getResourceAsStream(String, WebBrowser, com.vaadin.flow.theme.AbstractTheme)}.
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
