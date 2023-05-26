package com.vaadin.flow.prodbuild;

import jakarta.servlet.annotation.WebServlet;

import com.vaadin.flow.server.VaadinServlet;

@WebServlet(asyncSupported = true, urlPatterns = { "/*" })
public class RootMappingTestServlet extends VaadinServlet {
}
