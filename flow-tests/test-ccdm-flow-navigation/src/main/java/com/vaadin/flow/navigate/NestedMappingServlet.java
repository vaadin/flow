package com.vaadin.flow.navigate;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

@WebServlet(urlPatterns = { "/nested/*" })
public class NestedMappingServlet extends VaadinServlet {
}
