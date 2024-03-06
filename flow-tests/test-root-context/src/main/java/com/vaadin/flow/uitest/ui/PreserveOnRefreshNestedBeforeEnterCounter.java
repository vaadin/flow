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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

public class PreserveOnRefreshNestedBeforeEnterCounter extends Div
        implements BeforeEnterObserver {
    private int beforeEnterCount = 0;
    private final Span count = new Span();

    public PreserveOnRefreshNestedBeforeEnterCounter() {
        count.setId(getClass().getSimpleName() + "-before-enter-count");
        add(count);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        count.setText(Integer.toString(++beforeEnterCount));
    }
}
