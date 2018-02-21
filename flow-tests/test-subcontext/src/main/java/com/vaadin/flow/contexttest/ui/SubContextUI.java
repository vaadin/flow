package com.vaadin.flow.contexttest.ui;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletConfiguration;

public class SubContextUI extends DependencyUI {

    /**
     * The main servlet for the application.
     */
    @WebServlet(urlPatterns = {
            "/SubContext/*" }, name = "AnotherServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = SubContextUI.class, productionMode = false, usingNewRouting = false)
    public static class SubContextServlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {
        getElement().appendChild(ElementFactory.createDiv("Sub Context UI")
                .setAttribute("id", "sub"));
        super.init(request);
    }

}
