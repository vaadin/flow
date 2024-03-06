/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.scroll;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.ScrollableView")
public class ScrollableView extends Div {

    public static final String TEST_VIEW_ID = "ScrollableView";

    public ScrollableView() {
        setId(TEST_VIEW_ID);

        Span button = new Span("Click to scroll");
        button.setId("button");
        button.addClickListener(e -> {
            getComponentAt(500).scrollIntoView();
        });
        add(button);

        for (int i = 0; i < 1000; i++) {
            Div div = new Div();
            div.setId("div-" + i);
            div.setText("div-" + i);
            add(div);
        }

        getComponentAt(500).scrollIntoView();

    }
}
