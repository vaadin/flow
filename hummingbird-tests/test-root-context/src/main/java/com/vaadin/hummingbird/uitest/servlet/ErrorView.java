package com.vaadin.hummingbird.uitest.servlet;

import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.hummingbird.router.View;

public class ErrorView extends Div implements View {

    @Override
    public void onLocationChange(LocationChangeEvent locationChangeEvent) {
        getElement()
                .appendChild(
                        ElementFactory.createDiv(
                                "This is the error view. Next element contains the error path "),
                        ElementFactory
                                .createDiv(locationChangeEvent.getLocation()
                                        .getPath())
                                .setAttribute("id", "error-path"));
    }
}
