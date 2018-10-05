package com.vaadin.flow.noroot;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;

@Route
@HtmlImport("frontend://bower_components/polymer/polymer.html")
public class TestView extends Div {
    private static final String TEST_VIEW_ID = "testView";

    public TestView() {
        setId(TEST_VIEW_ID);
        getElement().executeJavaScript(String.format(
                "document.getElementById('%s').textContent = 'Polymer version: ' + Polymer.version",
                TEST_VIEW_ID));
    }

    @WebServlet(name = "customMappingServlet", urlPatterns = "/custom/*")
    public static class CustomMappingServlet extends VaadinServlet {

    }
}
