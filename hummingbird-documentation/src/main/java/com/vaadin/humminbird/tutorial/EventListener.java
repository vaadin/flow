package com.vaadin.humminbird.tutorial;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.ui.UI;

import elemental.json.JsonObject;

public abstract class EventListener extends UI {

    public void tutorialCode() {
        Element helloButton = new Element("button").setTextContent("Say hello");
        helloButton.addEventListener("click", e -> {
            Element response = new Element("div").setTextContent("Hello!");
            getElement().appendChild(response);
        });
        getElement().appendChild(helloButton);

        helloButton.addEventListener("click", e -> {
            JsonObject eventData = e.getEventData();
            boolean shiftKey = eventData.getBoolean("event.shiftKey");
            double width = eventData.getNumber("element.offsetWidth");

            String text = "Shift " + (shiftKey ? "down" : "up");
            text += " on button whose width is " + width + "px";

            Element response = new Element("div");
            response.setTextContent(text);
            getElement().appendChild(response);
        }, "event.shiftKey", "element.offsetWidth");
    }
}
