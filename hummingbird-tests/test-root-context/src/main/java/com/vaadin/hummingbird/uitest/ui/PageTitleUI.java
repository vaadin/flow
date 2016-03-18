package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class PageTitleUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        Element input = new Element("input");
        input.setAttribute("id", "input");
        input.setProperty("value", "");
        input.setSynchronizedProperties("value");
        input.setSynchronizedPropertiesEvents("change");

        Element updateButton = new Element("div");
        updateButton.setAttribute("id", "button");
        updateButton.setTextContent("Update page title");
        updateButton.addEventListener("click", e -> {
            getPage().updateTitle(input.getProperty("value"));
        });

        Element overrideButton = new Element("div");
        overrideButton.setAttribute("id", "override");
        overrideButton.setTextContent("Triggers two updates");
        overrideButton.addEventListener("click", e -> {
            getPage().updateTitle(input.getProperty("value"));
            getPage().updateTitle("OVERRIDDEN");
        });

        getElement().appendChild(input, updateButton, overrideButton);
    }

}
