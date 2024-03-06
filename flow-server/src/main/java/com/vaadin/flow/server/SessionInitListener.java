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
 * Event listener that can be registered to a {@link VaadinService} to get an
 * event when a new Vaadin service session is initialized for that service.
 * <p>
 * Because of the way different service instances share the same session, the
 * listener is not necessarily notified immediately when the session is created
 * but only when the first request for that session is handled by a specific
 * service.
 *
 * @see VaadinService#addSessionInitListener(SessionInitListener)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface SessionInitListener extends Serializable {
    /**
     * Invoked when a new Vaadin service session is initialized for that
     * service.
     * <p>
     * Because of the way different service instances share the same session,
     * the listener is not necessarily notified immediately when the session is
     * created but only when the first request for that session is handled by a
     * specific service.
     *
     * @param event
     *            the initialization event
     * @throws ServiceException
     *             a problem occurs when processing the event
     */
    void sessionInit(SessionInitEvent event) throws ServiceException;
}
