/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

/**
 * An event that is fired whenever a client polls the server for asynchronous UI
 * updates.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
