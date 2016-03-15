package com.vaadin.hummingbird.contexttest.ui;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;

public class SubContextUI extends DependencyUI {

    /**
     * The main servlet for the application.
     */
    @WebServlet(urlPatterns = {
            "/SubContext/*" }, name = "AnotherServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = SubContextUI.class, productionMode = false)
    public static class SubContextServlet extends VaadinServlet {
    }

    @Override
    protected void init(VaadinRequest request) {
        getElement().appendChild(new Element("div").setAttribute("id", "sub")
                .setTextContent("Sub Context UI"));
        super.init(request);
    }

    @Override
    protected String getServletToContextPath(String url) {
        return "../" + super.getServletToContextPath(url);
    }
}
