/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.HiddenTemplateView", layout = ViewTestLayout.class)
@Tag("hidden-template")
@JsModule("./HiddenTemplate.js")
public class HiddenTemplateView extends PolymerTemplate<TemplateModel> {

    @Id("hidden-child")
    private Div hiddenChild;

    @Id("child")
    private Div child;

    public HiddenTemplateView() {
        setId("template");
    }

    @EventHandler
    private void updateVisibility() {
        hiddenChild.setVisible(!hiddenChild.isVisible());
        child.setVisible(!child.isVisible());
    }

}
