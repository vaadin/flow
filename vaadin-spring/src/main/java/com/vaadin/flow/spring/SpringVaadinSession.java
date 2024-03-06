/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Vaadin session implementation for Spring.
 *
 * @author Vaadin Ltd
 *
 */
public class SpringVaadinSession extends VaadinSession {

    private final List<SessionDestroyListener> destroyListeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a new VaadinSession tied to a VaadinService.
     *
     * @param service
     *            the Vaadin service for the new session
     */
    public SpringVaadinSession(VaadinService service) {
        super(service);
    }

    /**
     * Handles destruction of the session.
     */
    public void fireSessionDestroy() {
        SessionDestroyEvent event = new SessionDestroyEvent(getService(), this);
        destroyListeners.stream()
                .forEach(listener -> listener.sessionDestroy(event));
        destroyListeners.clear();
    }

    /**
     * Adds a listener that gets notified when the Vaadin service session is
     * destroyed.
     * <p>
     * No need to remove the listener since all listeners are removed
     * automatically once session is destroyed
     * <p>
     * The session being destroyed is locked and its UIs have been removed when
     * the listeners are called.
     *
     * @see VaadinService#addSessionInitListener(SessionInitListener)
     *
     * @param listener
     *            the vaadin service session destroy listener
     */
    public void addDestroyListener(SessionDestroyListener listener) {
        destroyListeners.add(listener);
    }

}
