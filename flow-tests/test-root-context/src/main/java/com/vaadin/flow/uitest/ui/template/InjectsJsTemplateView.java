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
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.InjectsJsTemplateView", layout = ViewTestLayout.class)
@Tag("injects-js-template")
@JsModule("./InjectsJsTemplate.js")
public class InjectsJsTemplateView extends PolymerTemplate<TemplateModel> {

    @Id("injected-template")
    private JsInjectedElement injectedTemplate;

    @Id("injected-div")
    private JsInjectedDiv div;

    public InjectsJsTemplateView() {
        injectedTemplate.getElement().setProperty("baz", "setFromParent");
        div.addClassName("setFromParent");
    }

}
