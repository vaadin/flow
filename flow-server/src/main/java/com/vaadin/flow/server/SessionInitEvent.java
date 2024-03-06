/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.util.EventObject;

/**
 * Event gets fired when a new Vaadin service session is initialized for a
 * Vaadin service.
 * <p>
 * Because of the way different service instances share the same session, the
 * event is not necessarily fired immediately when the session is created but
 * only when the first request for that session is handled by a specific
 * service.
 *
 * @see SessionInitListener#sessionInit(SessionInitEvent)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SessionInitEvent extends EventObject {

    private final VaadinSession session;
    private final transient VaadinRequest request;

    /**
     * Creates a new event.
     *
     * @param service
     *            the Vaadin service from which the event originates
     * @param session
     *            the Vaadin service session that has been initialized
     * @param request
     *            the request that triggered the initialization
     */
    public SessionInitEvent(VaadinService service, VaadinSession session,
            VaadinRequest request) {
        super(service);
        this.session = session;
        this.request = request;
    }

    @Override
    public VaadinService getSource() {
        return (VaadinService) super.getSource();
    }

    /**
     * Gets the Vaadin service from which this event originates.
     *
     * @return the Vaadin service instance
     */
    public VaadinService getService() {
        return getSource();
    }

    /**
     * Gets the Vaadin service session that has been initialized.
     *
     * @return the Vaadin service session
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Gets the request that triggered the initialization.
     *
     * @return the request
     */
    public VaadinRequest getRequest() {
        return request;
    }

}
