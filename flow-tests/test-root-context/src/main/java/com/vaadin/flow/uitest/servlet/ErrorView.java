package com.vaadin.flow.uitest.servlet;

import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.router.ErrorParameter;
import com.vaadin.router.HasErrorParameter;
import com.vaadin.router.Route;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.BeforeNavigationListener;
import com.vaadin.ui.html.Div;

@Route(value = "ErrorView")
public class ErrorView extends Div implements BeforeNavigationListener,
        HasErrorParameter<ClassNotFoundException> {

    @Override
    public void beforeNavigation(BeforeNavigationEvent event) {
        getElement().appendChild(ElementFactory.createDiv(
                "This is the error view. Next element contains the error path "),
                ElementFactory.createDiv(event.getLocation().getPath())
                        .setAttribute("id", "error-path"));
    }

    @Override
    public int setErrorParameter(BeforeNavigationEvent event,
            ErrorParameter<ClassNotFoundException> parameter) {
        getElement().appendChild(ElementFactory.createDiv(
                "This is the error view. Next element contains the error path "),
                ElementFactory.createDiv(event.getLocation().getPath())
                        .setAttribute("id", "error-path"));
        return 0;
    }
}
