/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.component.internal.CompositionEvent;

/**
 * The event when a composition is started.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@DomEvent("compositionstart")
public class CompositionStartEvent extends CompositionEvent {

    /**
     * Creates a new composition event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     * @param data
     *            the string being composed
     * @param locale
     *            language code for the composition event, if available;
     *            otherwise, the empty string
     */
    public CompositionStartEvent(Component source, boolean fromClient,
            @EventData("event.data") String data,
            @EventData("event.locale") String locale) {
        super(source, fromClient, data, locale);
    }

    /**
     * Creates a new server-side composition event with no additional
     * information.
     *
     * @param source
     *            the component that fired the event
     */
    public CompositionStartEvent(Component source) {
        super(source);
    }
}
