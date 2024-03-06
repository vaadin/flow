/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.io.Serializable;

import com.vaadin.flow.shared.Registration;

/**
 * The interface for adding and removing {@link PollEvent} listeners.
 * <p>
 * By implementing this interface, a class publicly announces that it is able to
 * send {@link PollEvent PollEvents} whenever the client sends a periodic poll
 * message to the client, to check for asynchronous server-side modifications.
 *
 * @see UI#setPollInterval(int)
 * @since 1.0
 */
public interface PollNotifier extends Serializable {
    /**
     * Add a poll listener.
     * <p>
     * The listener is called whenever the client polls the server for
     * asynchronous UI updates.
     *
     * @see UI#setPollInterval(int)
     * @param listener
     *            the listener to add
     * @return a handle that can be used for removing the listener
     */
    default Registration addPollListener(
            ComponentEventListener<PollEvent> listener) {
        if (this instanceof Component) {
            return ComponentUtil.addListener((Component) this, PollEvent.class,
                    listener);
        } else {
            throw new IllegalStateException(String.format(
                    "The class '%s' doesn't extend '%s'. "
                            + "Make your implementation for the method '%s'.",
                    getClass().getName(), Component.class.getSimpleName(),
                    "addPollListener"));
        }
    }

}
