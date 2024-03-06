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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ElementInnerHtmlView", layout = ViewTestLayout.class)
public class ElementInnerHtmlView extends AbstractDivView {

    Div innerHtml;

    @Override
    protected void onShow() {
        innerHtml = new Div();
        innerHtml.setId("inner-html-field");
        add(createButton("Foo"), createButton("Boo"), getNullButton(),
                createToggleButton(), innerHtml);

    }

    private NativeButton createButton(String value) {
        NativeButton button = new NativeButton("Set value " + value,
                click -> innerHtml.getElement().setProperty("innerHTML",
                        String.format("<p>%s</p>", value)));
        button.setId("set-" + value.toLowerCase());
        return button;
    }

    private NativeButton getNullButton() {
        NativeButton button = new NativeButton("Set value null",
                click -> innerHtml.getElement().setProperty("innerHTML", null));
        button.setId("set-null");
        return button;
    }

    private NativeButton createToggleButton() {
        NativeButton button = new NativeButton("ToggleVisibility",
                click -> innerHtml.setVisible(!innerHtml.isVisible()));
        button.setId("toggle-visibility");
        return button;
    }

}
