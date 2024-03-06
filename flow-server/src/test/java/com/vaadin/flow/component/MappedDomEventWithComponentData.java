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
public class MappedDomEventWithComponentData extends ComponentEvent<Component> {

    private final Component component;

    public MappedDomEventWithComponentData(Component source, boolean fromClient,
            @EventData("component") Component component) {
        super(source, fromClient);
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }

}
