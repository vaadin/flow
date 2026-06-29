/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

/**
 * The type of an entry point (scanned for frontend dependencies).
 * <p>
 * The "real" entry points are routes and exported web components. In addition
 * to those, there are a bunch of internal entry points that are also scanned.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.1
 */
public enum EntryPointType {
    ROUTE, WEB_COMPONENT, INTERNAL;
}
