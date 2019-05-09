package com.vaadin.flow.npmtest;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

/**
 * All other ITs are run with `bowerMode=true` and it's not possible to
 * configure each test module with its own value at pom.xml level.
 *
 * It should be possible to add a servlet fragment to each test in the suite,
 * but seems simpler so far to have this here to override default system
 * property just for npm test.
 */
@WebServlet(asyncSupported = true, urlPatterns = { "/*" })
public class NpmEnabledVaadinServlet extends VaadinServlet {
    public NpmEnabledVaadinServlet() {
        System.clearProperty("vaadin.bowerMode");
    }
}
