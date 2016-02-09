package com.vaadin.hummingbird.dom;

import java.io.Serializable;

/**
 * Handle used for removing an added event listener.
 *
 * @since
 * @author Vaadin Ltd
 */
public interface EventRegistrationHandle extends Serializable {
    /**
     * Removes the event listener.
     */
    public void remove();
}