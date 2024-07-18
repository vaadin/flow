/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.dependencies;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.dependencies.AnnotatedFrontendInlineView", layout = ViewTestLayout.class)
@Tag("frontend-inline")
@JavaScript(value = "components/frontend-inline.js", loadMode = LoadMode.INLINE)
@StyleSheet(value = "components/frontend-inline.css", loadMode = LoadMode.INLINE)
public class AnnotatedFrontendInlineView
        extends PolymerTemplate<TemplateModel> {

    public AnnotatedFrontendInlineView() {
        setId("template");
    }
}
