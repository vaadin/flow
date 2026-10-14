/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.hilla;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * A container for utility methods related with Hilla endpoints.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 23.2
 */
public interface EndpointRequestUtil extends Serializable {

    String HILLA_ENDPOINT_CLASS = "com.vaadin.hilla.EndpointController";

    /**
     * Checks if the request is for an endpoint.
     * <p>
     * Note even if this method returns <code>true</code>, there is no guarantee
     * that an endpoint method will actually be called, e.g. access might be
     * denied.
     *
     * @param request
     *            the HTTP request
     * @return <code>true</code> if the request is for an endpoint,
     *         <code>false</code> otherwise
     */
    boolean isEndpointRequest(HttpServletRequest request);

    /**
     * Checks if the given request goes to an anonymous (public) endpoint.
     *
     * @param request
     *            the HTTP request to check
     * @return <code>true</code> if the request goes to an anonymous endpoint,
     *         <code>false</code> otherwise
     */
    boolean isAnonymousEndpoint(HttpServletRequest request);

    /**
     * Checks if Hilla is available.
     *
     * @return true if Hilla is available, false otherwise
     */
    static boolean isHillaAvailable() {
        try {
            Class.forName(HILLA_ENDPOINT_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if Hilla is available using the given class finder.
     *
     * @param classFinder
     *            class finder to check the presence of Hilla endpoint class
     * @return true if Hilla is available, false otherwise
     */
    static boolean isHillaAvailable(ClassFinder classFinder) {
        try {
            classFinder.loadClass(HILLA_ENDPOINT_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
