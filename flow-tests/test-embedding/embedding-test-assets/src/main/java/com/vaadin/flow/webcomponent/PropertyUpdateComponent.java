/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

@Tag("click-counter")
public class PropertyUpdateComponent extends Div {
    @FunctionalInterface
    public interface NumberListener {
        void handleNumber(int number);
    }

    private int clickCounter = 0;
    private Set<NumberListener> listenerSet = new HashSet<>();

    public PropertyUpdateComponent() {
        NativeButton nativeButton = new NativeButton("Click me!");
        nativeButton.addClickListener(event -> {
            clickCounter++;
            publishNumber();
        });

        add(nativeButton);
    }

    private void publishNumber() {
        listenerSet.forEach(
                numberListener -> numberListener.handleNumber(clickCounter));
    }

    public JsonValue getNumberJson() {
        JsonObject json = Json.createObject();
        json.put("counter", clickCounter);
        return json;
    }

    public void addListener(NumberListener listener) {
        listenerSet.add(listener);
    }
}
