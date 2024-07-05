/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;

@Route(value = "com.vaadin.flow.uitest.ui.theme.CustomStylesView")
@Theme(MyTheme.class)
@HtmlImport("styles/shared-styles.html")
public class CustomStylesView extends Div {

    public CustomStylesView() {
        setText("Custom font size");
        setId("custom-style");
    }
}
