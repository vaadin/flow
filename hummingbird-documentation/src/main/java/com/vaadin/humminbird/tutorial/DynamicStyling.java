package com.vaadin.humminbird.tutorial;

import com.vaadin.hummingbird.dom.Element;

public class DynamicStyling {

    Element button = new Element("div");

    public void tutorialCode() {

        button.setTextContent("Change to blue");
        button.addEventListener("click", e -> {
            button.getClassList().add("blue");
        });

        Element input = new Element("input");
        button.setTextContent("Change to the entered value");
        button.addEventListener("click", e -> {
            button.getStyle().set("background", input.getProperty("value"));
        });
    }
}
