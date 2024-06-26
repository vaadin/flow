/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;

@Theme(themeFolder = "app-theme")
@Route(value = "com.vaadin.flow.uitest.ui.ThemeLiveReloadView")
public class ThemeLiveReloadView extends Div {

    public ThemeLiveReloadView() {
        add(new Paragraph("This is a Paragraph to test the applied font"));
    }
}
