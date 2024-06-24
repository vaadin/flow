/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@PreserveOnRefresh
@Route("preserve")
public class PreserveOnRefreshView extends Div
        implements AfterNavigationObserver {

    private final Div uiId;

    public PreserveOnRefreshView() {
        uiId = new Div();
        uiId.setId("uiId");
        NativeButton reloadButton = new NativeButton("Reload page",
                ev -> UI.getCurrent().getPage().reload());
        reloadButton.setId("reload");
        add(new H1("This view is preserved on refresh"));
        H3 initialUIId = new H3("Initial UI: " + UI.getCurrent().getUIId());
        initialUIId.setId("initialUIId");
        add(initialUIId);
        add(uiId, reloadButton);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        uiId.setText("UI: " + UI.getCurrent().getUIId());
    }
}
