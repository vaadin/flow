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
     * Resolves the given uri to a path which can be used with
     * {@link VaadinService#getResource(String)} and
     * {@link VaadinService#getResourceAsStream(String)}.
     *
     * @param uri
     *            the URI to resolve
     * @return the URI resolved to be relative to the context root
     */
    public String resolveVaadinUri(String uri) {
        return super.resolveVaadinUri(uri, "/");
    }

}
