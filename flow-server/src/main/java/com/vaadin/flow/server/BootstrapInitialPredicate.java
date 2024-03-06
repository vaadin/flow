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
 * The callback used by bootstrap handlers in order to know when a request needs
 * to pre-render the UI and include the initial UIDL in the page.
 *
 * @since 3.0
 */
@FunctionalInterface
public interface BootstrapInitialPredicate extends EventListener, Serializable {

    /**
     * Return whether the bootstrap handler has to include initial UIDL in the
     * response.
     *
     * @param request
     *            Vaadin request.
     * @return true if initial should be included
     */
    boolean includeInitialUidl(VaadinRequest request);

}
