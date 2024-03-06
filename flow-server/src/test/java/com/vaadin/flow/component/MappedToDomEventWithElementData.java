/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import com.vaadin.flow.dom.Element;

@DomEvent("dom-event")
public class MappedToDomEventWithElementData extends ComponentEvent<Component> {

    private final Element element;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source
     *            the source component
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     */
    public MappedToDomEventWithElementData(Component source, boolean fromClient,
            @EventData("element") Element element) {
        super(source, fromClient);
        this.element = element;
    }

    public Element getElement() {
        return element;
    }
}
