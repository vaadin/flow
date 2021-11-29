package com.vaadin.flow.server;

import com.vaadin.flow.component.UI;

public class MockUIContainingServlet extends UI {

    public static class ServletInUI extends VaadinServlet {
        // This servlet should automatically be configured to use the
        // enclosing UI class
    }

    @Override
    protected void init(VaadinRequest request) {
        // Do nothing
    }
}
