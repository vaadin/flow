/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("parent-template")
@JsModule("./ParentTemplate.js")
@Route("parent-template")
public class TemplateInjectsTemplate extends PolymerTemplate<TemplateModel> {

    @Id("child")
    private ChildTemplate template;

    @Id("div")
    private Div div;

    public TemplateInjectsTemplate() {
        template.getElement().setProperty("foo", "bar");
        div.setText("baz");
    }
}
