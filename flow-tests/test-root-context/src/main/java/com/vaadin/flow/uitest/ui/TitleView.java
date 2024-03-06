/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.TitleView", layout = ViewTestLayout.class)
@PageTitle("Title view")
public class TitleView extends AbstractDivView {

    @Override
    protected void onShow() {
        removeAll();
        Div titleView = new Div();
        titleView.setText("Title view");
        titleView.setId("annotated");
        add(titleView);
    }

}
