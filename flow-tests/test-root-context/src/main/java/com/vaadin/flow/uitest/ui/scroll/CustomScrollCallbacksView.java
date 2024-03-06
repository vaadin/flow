/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.scroll;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.scroll.CustomScrollCallbacksView", layout = ViewTestLayout.class)
public class CustomScrollCallbacksView extends AbstractDivView
        implements HasUrlParameter<String> {
    private final Div viewName = new Div();
    private final Div log = new Div();

    public CustomScrollCallbacksView() {
        viewName.setId("view");

        log.setId("log");
        log.getStyle().set("white-space", "pre");

        UI.getCurrent().getPage().executeJs(
                "window.Vaadin.Flow.setScrollPosition = function(xAndY) { $0.textContent += JSON.stringify(xAndY) + '\\n' }",
                log);
        UI.getCurrent().getPage().executeJs(
                "window.Vaadin.Flow.getScrollPosition = function() { return [42, -window.pageYOffset] }");

        RouterLink navigate = new RouterLink("Navigate",
                CustomScrollCallbacksView.class, "navigated");
        navigate.setId("navigate");

        Anchor back = new Anchor("javascript:history.go(-1)", "Back");
        back.setId("back");

        add(viewName, log, new Span("Scroll down to see navigation actions"),
                ScrollView.createSpacerDiv(2000), navigate, back);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        viewName.setText("Current view: " + parameter);
    }
}
