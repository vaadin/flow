/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.noroot;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinServlet;

@Route("")
@PWA(name = "testView", shortName = "tw")
public class NoRootTestView extends Div {
    static final String TEST_VIEW_ID = "testView";

    public NoRootTestView() {
        setId(TEST_VIEW_ID);
        getElement().executeJavaScript(String.format(
                "document.getElementById('%s').textContent = 'Polymer version: ' + Polymer.version",
                TEST_VIEW_ID));
    }

    @WebServlet(name = "customMappingServlet", urlPatterns = "/custom/*")
    public static class CustomMappingServlet extends VaadinServlet {
    }
}