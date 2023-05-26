package com.vaadin.flow.prodbuild;

import jakarta.servlet.annotation.WebServlet;

@WebServlet(asyncSupported = true, urlPatterns = { "/*" })
public class RootMappingTestServlet {
}
