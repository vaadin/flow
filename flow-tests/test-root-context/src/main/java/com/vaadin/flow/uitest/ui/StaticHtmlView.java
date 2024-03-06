/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.StaticHtmlView", layout = ViewTestLayout.class)
public class StaticHtmlView extends Html {

    public StaticHtmlView() {
        super(StaticHtmlView.class.getResourceAsStream("StaticHtmlView.html"));
    }

}
