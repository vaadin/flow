/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.theme.Theme;

@Theme(themeFolder = "embedded-theme")
@NpmPackage(value = "@fortawesome/fontawesome-free", version = "5.15.1")
public class ThemedComponentExporter
        extends WebComponentExporter<ThemedComponent> {
    public ThemedComponentExporter() {
        super("themed-component");
    }

    @Override
    public void configureInstance(WebComponent<ThemedComponent> webComponent,
            ThemedComponent component) {

    }
}
