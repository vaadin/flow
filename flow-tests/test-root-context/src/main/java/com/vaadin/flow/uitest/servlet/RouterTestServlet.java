package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Route;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.flow.html.Div;
import com.vaadin.server.VaadinServlet;

@WebServlet(asyncSupported = true, urlPatterns = { "/new-router-session/*" })
@VaadinServletConfiguration(productionMode = false, usingNewRouting = true)
public class RouterTestServlet extends VaadinServlet {

    public static class ClassNameDiv extends Div {
        public ClassNameDiv() {
            setText(this.getClass().getSimpleName());
            setId("name-div");
        }
    }

    @Route("")
    public static class RootNavigationTarget extends ClassNameDiv {
    }

    @Route("foo")
    public static class FooNavigationTarget extends ClassNameDiv {
    }

    @Route("foo/bar")
    public static class FooBarNavigationTarget extends ClassNameDiv {
    }
}
