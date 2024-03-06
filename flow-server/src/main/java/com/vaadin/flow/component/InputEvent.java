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
 * Event fired when the component has received any type of input (e.g. click,
 * key press).
 * <p>
 * This event is specifically intended to the used for the <code>input</code>
 * event in the DOM API.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@DomEvent("input")
public class InputEvent extends ComponentEvent<Component> {
    /**
     * Creates a new input event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     */
    public InputEvent(Component source, boolean fromClient) {
        super(source, fromClient);
    }
}
