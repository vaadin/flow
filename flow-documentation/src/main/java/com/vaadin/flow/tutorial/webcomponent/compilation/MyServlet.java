package com.vaadin.flow.tutorial.webcomponent.compilation;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletConfiguration;

@CodeFor("miscellaneous/tutorial-flow-runtime-configuration.asciidoc")
@WebServlet(urlPatterns = "/*", name = "myservlet", asyncSupported = true, initParams = {
        @WebInitParam(name = "frontend.url.es6", value = "http://mydomain.com/es6/"),
        @WebInitParam(name = "frontend.url.es5", value = "http://mydomain.com/es5/") })
@VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
public class MyServlet extends VaadinServlet {
}
