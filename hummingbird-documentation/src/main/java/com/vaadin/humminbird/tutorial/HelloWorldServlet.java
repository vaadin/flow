package com.vaadin.humminbird.tutorial;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.server.VaadinServlet;

@CodeFor("tutorial-hello-world.asciidoc")
@WebServlet(urlPatterns = "/*", name = "HelloWorldServlet")
@VaadinServletConfiguration(ui = HelloWorldUI.class, productionMode = false)
public class HelloWorldServlet extends VaadinServlet {
}