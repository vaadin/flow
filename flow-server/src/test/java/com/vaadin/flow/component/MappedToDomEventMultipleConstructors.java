/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

@DomEvent("dom-event")
public class MappedToDomEventMultipleConstructors
        extends ComponentEvent<Component> {

    public MappedToDomEventMultipleConstructors(Component source,
            boolean fromClient, @EventData("someParam") int someParam) {
        super(source, fromClient);
    }

    public MappedToDomEventMultipleConstructors(Component source,
            boolean fromClient, @EventData("otherParam") boolean otherParam) {
        super(source, fromClient);
    }

}
