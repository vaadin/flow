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
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.InjectedElementInsideMixinBehaviorView", layout = ViewTestLayout.class)
@Tag("mixin-injects")
@NpmPackage(value = "@polymer/iron-resizable-behavior", version = "3.0.1")
@JsModule("./MixinInjectsElement.js")
public class InjectedElementInsideMixinBehaviorView
        extends PolymerTemplate<TemplateModel> {

    @Id("injected")
    private Div div;

    public InjectedElementInsideMixinBehaviorView() {
        div.setText("foo");
    }
}
