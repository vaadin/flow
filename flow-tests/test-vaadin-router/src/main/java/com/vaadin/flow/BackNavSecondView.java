/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.BackNavSecondView")
public class BackNavSecondView extends Div implements AfterNavigationObserver {

    public static final String CALLS = "calls";
    private int count = 0;
    Span text = new Span("Second view: " + count);

    public BackNavSecondView() {
        text.setId(CALLS);
        add(text);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        count++;
        text.setText("Second view: " + count);
        UI.getCurrent().getPage().getHistory().replaceState(null,
                "com.vaadin.flow.BackNavSecondView?param");
    }
}
