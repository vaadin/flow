/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;

@Tag("css-import-component")
@CssImport("./css-import-component.css")
public class CssImportComponent extends Div {

    public CssImportComponent(String id) {
        setId(id);
        Div div = new Div(
                "Global CssImport styles should be applied inside embedded web component, this should not be black");
        div.setClassName("cssimport");
        add(div);
    }
}
