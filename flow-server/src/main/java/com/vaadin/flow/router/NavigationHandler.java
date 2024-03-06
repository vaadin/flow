/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * Handles navigation to a location e.g. by showing a navigation target
 * component in a {@link UI} or by redirecting the user to another location.
 * <p>
 * Subclasses using external data should take care to avoid synchronization
 * issues since the same navigation handler instances may be used concurrently
 * from multiple threads. Data provided in the navigation event should be safe
 * to use without synchronization since the associated {@link VaadinSession} and
 * everything related to it will be locked.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface NavigationHandler extends Serializable {
    /**
     * Handles the navigation event.
     *
     * @param event
     *            the navigation event to handle
     * @return the HTTP status code to return to the client if handling an
     *         initial rendering request
     */
    int handle(NavigationEvent event);

}
