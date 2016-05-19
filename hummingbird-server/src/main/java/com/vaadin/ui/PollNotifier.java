package com.vaadin.ui;

import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.event.ComponentEventListener;

/**
 * The interface for adding and removing {@link PollEvent} listeners.
 * <p>
 * By implementing this interface, a class publicly announces that it is able to
 * send {@link PollEvent PollEvents} whenever the client sends a periodic poll
 * message to the client, to check for asynchronous server-side modifications.
 *
 * @since 7.2
 * @see UI#setPollInterval(int)
 */
public interface PollNotifier extends ComponentEventNotifier {
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
    default EventRegistrationHandle addPollListener(
            ComponentEventListener<PollEvent> listener) {
        return addListener(PollEvent.class, listener);
    }

}
