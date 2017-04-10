package com.vaadin.flow.uitest.servlet;

import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.View;

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
