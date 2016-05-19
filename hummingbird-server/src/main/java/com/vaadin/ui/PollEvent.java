package com.vaadin.ui;

import com.vaadin.annotations.DomEvent;

/**
 * An event that is fired whenever a client polls the server for asynchronous UI
 * updates.
 *
 * @since 7.2
 * @author Vaadin Ltd
 */
@DomEvent(PollEvent.DOM_EVENT_NAME)
public class PollEvent extends ComponentEvent<UI> {
    public static final String DOM_EVENT_NAME = "ui-poll";

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param ui
     *            the source UI
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     *            side, <code>false</code> otherwise
     */
    public PollEvent(UI ui, boolean fromClient) {
        super(ui, fromClient);
    }

}
