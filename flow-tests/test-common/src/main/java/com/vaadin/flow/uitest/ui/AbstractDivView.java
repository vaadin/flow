/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

public abstract class AbstractDivView extends Div
        implements BeforeEnterObserver {

    public AbstractDivView() {
    }

    protected void onShow() {

    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        onShow();
    }

    protected Page getPage() {
        // getUI not available in onLocationChange so leaving getCurrent here
        // for now
        return UI.getCurrent().getPage();
    }

    public static NativeButton createButton(String caption, String id,
            ComponentEventListener<ClickEvent<NativeButton>> listener) {
        NativeButton button = new NativeButton(caption, listener);
        button.setId(id);
        return button;
    }
}
