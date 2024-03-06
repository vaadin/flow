/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import javax.servlet.annotation.WebServlet;
import java.io.PrintWriter;
import java.util.function.Consumer;

import com.vaadin.flow.webcomponent.servlets.AbstractPlainServlet;

// npm mode is able to survive a root-mapped servlet, while compatibility
// mode is not
@WebServlet(urlPatterns = { "/*", "/items/*" }, asyncSupported = true)
public class NpmPlainServlet extends AbstractPlainServlet {
    @Override
    protected Consumer<PrintWriter> getImportsWriter() {
        return this::writeNpmImports;
    }
}
