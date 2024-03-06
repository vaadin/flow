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
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateInTemplateWithIdView", layout = ViewTestLayout.class)
public class TemplateInTemplateWithIdView extends AbstractDivView {

    @Tag("parent-id-template")
    @JsModule("./ParentIdTemplate.js")
    @Uses(ChildTemplate.class)
    public static class ParentTemplate extends PolymerTemplate<TemplateModel> {

    }

    @Tag("child-id-template")
    @JsModule("./ChildIdTemplate.js")
    public static class ChildTemplate extends PolymerTemplate<TemplateModel> {
        @Id("text")
        Div div;

        public ChildTemplate() {
            div.setText("@Id injected!");
        }
    }

    @Override
    protected void onShow() {
        ParentTemplate template = new ParentTemplate();
        template.setId("template");
        add(template);
    }
}
