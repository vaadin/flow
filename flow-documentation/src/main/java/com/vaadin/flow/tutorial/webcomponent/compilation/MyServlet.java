package com.vaadin.flow.tutorial.webcomponent.compilation;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.server.VaadinServlet;

@CodeFor("web-components/tutorial-webcomponents-es5.asciidoc")
@WebServlet(urlPatterns = "/*", name = "myservlet", asyncSupported = true, initParams = {
        @WebInitParam(name = "frontend.url.es6", value = "http://mydomain.com/es6/"),
        @WebInitParam(name = "frontend.url.es5", value = "http://mydomain.com/es5/") })
@VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
public class MyServlet extends VaadinServlet {
}
