package com.vaadin.flow.uitest.servlet;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteNotFoundError;
import com.vaadin.flow.router.BeforeNavigationEvent;

public class ErrorTarget extends RouteNotFoundError {

    @Override
    public int setErrorParameter(BeforeNavigationEvent event,
            ErrorParameter<NotFoundException> parameter) {
        getElement().appendChild(ElementFactory.createDiv(
                "This is the error view. Next element contains the error path "),
                ElementFactory.createDiv(event.getLocation().getPath())
                        .setAttribute("id", "error-path"));
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
