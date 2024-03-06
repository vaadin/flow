/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.BaseHrefView", layout = ViewTestLayout.class)
public class BaseHrefView extends AbstractDivView
        implements HasUrlParameter<String> {

    public BaseHrefView() {
        add(new Anchor("link", "My link"));
    }

    @Override
    public void setParameter(BeforeEvent event,
            @WildcardParameter String parameter) {
        // Noop
    }
}
