/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import com.vaadin.flow.component.Component;

/**
 * Client route target stores the target template.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.5
 */
public class ClientTarget extends RouteTarget {
    private final String template;

    /**
     * Create a new Client route target holder with the given route template.
     *
     * @param template
     *            route template
     */
    public ClientTarget(String template) {
        super(Component.class);
        this.template = template;
    }

    /**
     * Get the route template.
     *
     * @return route template
     */
    String getTemplate() {
        return template;
    }
}
