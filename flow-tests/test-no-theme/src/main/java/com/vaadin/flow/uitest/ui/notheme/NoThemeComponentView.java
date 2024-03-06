/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.notheme;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.notheme.NoThemeComponentView")
public class NoThemeComponentView extends Div {

    public NoThemeComponentView() {
        Anchor anchor = new Anchor();
        anchor.setText("Hello notheme");
        add(anchor);
    }
}
