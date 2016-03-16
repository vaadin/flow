package com.vaadin.humminbird.tutorial;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class IncludeCss extends UI {

    @Override
    protected void init(VaadinRequest request) {
        // Loaded from "styles.css" in our context root
        getPage().addStyleSheet("styles.css");

        // Loaded from "/root.css" regardless of how our application is deployed
        getPage().addStyleSheet("/root.css");

        // Loaded from "http://example.com/example.css" regardless of where our
        // application is deployed
        getPage().addStyleSheet("http://example.com/example.css");
    }

}
