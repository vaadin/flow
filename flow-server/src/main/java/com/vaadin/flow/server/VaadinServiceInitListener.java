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
import java.util.ServiceLoader;

/**
 * Listener for {@link VaadinService} initialization events. The listener can
 * add listeners and request handlers to the service.
 * <p>
 * Listener instances are by default discovered and instantiated using
 * {@link ServiceLoader}. This means that all implementations must have a
 * zero-argument constructor and the fully qualified name of the implementation
 * class must be listed on a separate line in a
 * META-INF/services/com.vaadin.flow.server.VaadinServiceInitListener file
 * present in the jar file containing the implementation class.
 * <p>
 * Integrations for specific runtime environments, such as OSGi or Spring, might
 * also provide other ways of discovering listeners.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface VaadinServiceInitListener extends EventListener, Serializable {
    /**
     * Run when a {@link VaadinService} instance is initialized.
     *
     * @param event
     *            the service initialization event
     */
    void serviceInit(ServiceInitEvent event);
}
