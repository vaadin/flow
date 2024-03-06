/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.dependencies.AnnotatedFrontendInlineView", layout = ViewTestLayout.class)
@JavaScript(value = "/components/context-inline.js", loadMode = LoadMode.INLINE)
@StyleSheet(value = "/components/context-inline.css", loadMode = LoadMode.INLINE)
public class AnnotatedContextInlineView extends Div {

    public AnnotatedContextInlineView() {
        setId("template");
    }
}
