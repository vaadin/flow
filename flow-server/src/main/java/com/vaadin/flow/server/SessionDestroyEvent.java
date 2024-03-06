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
 * Event fired when a Vaadin service session is no longer in use.
 *
 * @see SessionDestroyListener#sessionDestroy(SessionDestroyEvent)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SessionDestroyEvent extends EventObject {

    private final VaadinSession session;

    /**
     * Creates a new event.
     *
     * @param service
     *            the Vaadin service from which the even originates
     * @param session
     *            the Vaadin service session that is no longer used
     */
    public SessionDestroyEvent(VaadinService service, VaadinSession session) {
        super(service);
        this.session = session;
    }

    @Override
    public VaadinService getSource() {
        return (VaadinService) super.getSource();
    }

    /**
     * Gets the Vaadin service from which the even originates.
     *
     * @return the Vaadin service
     */
    public VaadinService getService() {
        return getSource();
    }

    /**
     * Gets the Vaadin service session that is no longer used.
     *
     * @return the Vaadin service session
     */
    public VaadinSession getSession() {
        return session;
    }

}
