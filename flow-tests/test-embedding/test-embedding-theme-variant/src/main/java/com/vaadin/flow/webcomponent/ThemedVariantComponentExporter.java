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
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.theme.Theme;

@Theme(themeClass = VariantTheme.class, variant = VariantTheme.DARK)
public class ThemedVariantComponentExporter
        extends WebComponentExporter<ThemedVariantComponent> {
    public ThemedVariantComponentExporter() {
        super("themed-variant-web-component");
    }

    @Override
    public void configureInstance(
            WebComponent<ThemedVariantComponent> webComponent,
            ThemedVariantComponent component) {
    }
}
