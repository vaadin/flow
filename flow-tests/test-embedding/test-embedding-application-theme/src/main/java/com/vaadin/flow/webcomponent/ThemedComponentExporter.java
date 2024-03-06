/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.theme.Theme;

import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

@Theme("embedded-theme")
@NpmPackage(value = "@fortawesome/fontawesome-free", version = TestVersion.FONTAWESOME)
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
