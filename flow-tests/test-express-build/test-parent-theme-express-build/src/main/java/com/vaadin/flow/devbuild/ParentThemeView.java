/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.devbuild;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.devbuild.ParentThemeView", layout = ViewTestLayout.class)
public class ParentThemeView extends Div {

    public ParentThemeView() {
        Image snowFlake = new Image(
                "themes/reusable-theme/fortawesome/icons/snowflake.svg",
                "snowflake");
        snowFlake.setHeight("5em");
        snowFlake.setId("snowflake");
        add(snowFlake);

        add(new Paragraph(
                "This is a Paragraph to test the applied parent styles"));
    }
}
