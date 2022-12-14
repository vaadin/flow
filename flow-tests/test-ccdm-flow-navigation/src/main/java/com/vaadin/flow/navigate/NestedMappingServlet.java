package com.vaadin.flow.navigate;

import jakarta.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

@WebServlet(urlPatterns = { "/nested/*" })
public class NestedMappingServlet extends VaadinServlet {
}
