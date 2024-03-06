/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.DynamicTitleView", layout = ViewTestLayout.class)
public class DynamicTitleView extends AbstractDivView
        implements HasDynamicTitle {

    @Override
    protected void onShow() {
        getElement().removeAllChildren().appendChild(new Element(Tag.DIV)
                .setText("Dynamic").setAttribute("id", "dynamic"));
    }

    @Override
    public String getPageTitle() {
        return "dynamic title view";
    }

}
