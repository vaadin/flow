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
import java.util.EventListener;

/**
 * The callback used by bootstrap handlers in order to know when a request is a
 * valid URL to render the page.
 *
 * @since 3.0
 */
@FunctionalInterface
public interface BootstrapUrlPredicate extends EventListener, Serializable {

    /**
     * Return whether the bootstrap handler should render the page.
     *
     * @param request
     *            Vaadin request.
     * @return true if the page should be rendered.
     */
    boolean isValidUrl(VaadinRequest request);
}
