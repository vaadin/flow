/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.shared;

import java.io.Serializable;

/**
 * Utility for translating special Vaadin URIs into URLs usable by the browser.
 * This is an abstract class performing the main logic in
 * {@link #resolveVaadinUri(String, String)}.
 * <p>
 * Concrete implementations of this class should implement {@link Serializable}
 * in case a reference to an object of this class is stored on the server side.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class VaadinUriResolver implements Serializable {

    /**
     * Translates a Vaadin URI to a URL that can be loaded by the browser. The
     * following URI schemes are supported:
     * <ul>
     * <li><code>{@value com.vaadin.flow.shared.ApplicationConstants#CONTEXT_PROTOCOL_PREFIX}</code>
     * resolves to the application context root</li>
     * <li><code>{@value com.vaadin.flow.shared.ApplicationConstants#BASE_PROTOCOL_PREFIX}</code>
     * - resolves to the base URI of the page</li>
     * </ul>
     * Any other URI protocols, such as <code>http://</code> or
     * <code>https://</code> are passed through this method unmodified.
     *
     * @param uri
     *            the URI to resolve
     * @param servletToContextRoot
     *            the relative path from the servlet path (used as base path in
     *            the client) to the context root
     * @return the resolved URI
     */
    protected String resolveVaadinUri(String uri, String servletToContextRoot) {
        if (uri == null) {
            return null;
        }

        String processedUri = processProtocol(
                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX,
                servletToContextRoot, uri);
        processedUri = processProtocol(
                ApplicationConstants.BASE_PROTOCOL_PREFIX, "", processedUri);

        return processedUri;
    }

    private String processProtocol(String protocol, String replacement,
            String vaadinUri) {
        if (vaadinUri.startsWith(protocol)) {
            vaadinUri = replacement + vaadinUri.substring(protocol.length());
        }
        return vaadinUri;
    }

}
