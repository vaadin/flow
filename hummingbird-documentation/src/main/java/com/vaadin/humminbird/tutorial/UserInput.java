package com.vaadin.humminbird.tutorial;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.ui.UI;

public abstract class UserInput extends UI {

    public void tutorialCode() {
        Element textInput = new Element("input");
        textInput.setAttribute("placeholder", "Please enter your name");

        Element button = new Element("div");
        button.addEventListener("click", e -> {
            String responseText = "Hello " + textInput.getProperty("value");
            Element response = new Element("div").setTextContent(responseText);
            getElement().appendChild(response);
        });
    }
}
