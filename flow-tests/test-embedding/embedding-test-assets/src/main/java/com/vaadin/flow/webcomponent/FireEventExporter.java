/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.EventOptions;
import com.vaadin.flow.component.webcomponent.WebComponent;

import elemental.json.Json;

public class FireEventExporter
        extends WebComponentExporter<FireEventComponent> {

    public FireEventExporter() {
        super("fire-event");
    }

    @Override
    public void configureInstance(WebComponent<FireEventComponent> webComponent,
            FireEventComponent component) {
        component.setSumConsumer(number -> webComponent
                .fireEvent("sum-calculated", Json.create(number)));
        component.setErrorConsumer(err -> webComponent
                .fireEvent("sum" + "-error", Json.create(err)));

        component.setButtonConsumer(optionsType -> {
            EventOptions options = createEventOptions(optionsType);
            webComponent.fireEvent("button-event",
                    Json.create(optionsType.name()), options);
        });
    }

    private EventOptions createEventOptions(FireEventComponent.OptionsType id) {
        switch (id) {
        case NoBubble_NoCancel:
            return new EventOptions(false, false, false);
        case Bubble_NoCancel:
            return new EventOptions(true, false, false);
        case Bubble_Cancel:
            return new EventOptions(true, true, false);
        }
        return new EventOptions();
    }
}
