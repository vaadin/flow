/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.dependencies.ContextInlineApiView", layout = ViewTestLayout.class)
public class ContextInlineApiView extends Div {

    public ContextInlineApiView() {
        setId("template");
        UI.getCurrent().getPage().addJavaScript("/components/context-inline.js",
                LoadMode.INLINE);
        UI.getCurrent().getPage().addStyleSheet(
                "/components/context-inline.css", LoadMode.INLINE);
    }
}
