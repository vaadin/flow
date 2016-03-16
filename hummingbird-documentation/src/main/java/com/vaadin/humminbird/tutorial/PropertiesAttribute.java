package com.vaadin.humminbird.tutorial;

import com.vaadin.hummingbird.dom.Element;

public class PropertiesAttribute {

    public void tutorialCode() {
        Element nameField = new Element("input");
        nameField.setAttribute("id", "nameField");
        nameField.setAttribute("placeholder", "John Doe");
        nameField.setAttribute("autofocus", "");

        // "John Doe"
        String placeholder = nameField.getAttribute("placeholder");

        // true
        nameField.hasAttribute("autofocus");

        nameField.removeAttribute("autofocus");

        // ["id", "placeholder"]
        nameField.getAttributeNames().toArray();

        Element element = new Element("input");
        element.setProperty("value", "Hello");
    }
}
