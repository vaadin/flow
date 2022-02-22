package com.vaadin.flow.navigate;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

@WebServlet(urlPatterns = { "/*" })
public class DefaultMappingServlet extends VaadinServlet {
}
