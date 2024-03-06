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

/**
 * Listener that gets notified when the {@link VaadinService} to which it has
 * been registered is destroyed.
 *
 * @see VaadinService#addServiceDestroyListener(ServiceDestroyListener)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface ServiceDestroyListener extends Serializable {
    /**
     * Invoked when a service is destroyed.
     *
     * @param event
     *            the event
     */
    void serviceDestroy(ServiceDestroyEvent event);
}
