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

/**
 * A container for utility methods related with Hilla file-based router.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 24.4
 */
public interface FileRouterRequestUtil {
    /**
     * Checks if the request corresponds to a Hilla route and, if so, applies
     * the corresponding access control.
     *
     * @param request
     *            the HTTP request to check
     * @return {@code true} if the request is allowed, {@code false} otherwise
     */
    boolean isRouteAllowed(HttpServletRequest request);
}
