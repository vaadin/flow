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
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("js-sub-template")
@JsModule("./JsSubTemplate.js")
public class JsSubTemplate
        extends PolymerTemplate<JsSubTemplate.JsSubTemplateModel> {

    @Id("js-grand-child")
    private JsInjectedGrandChild component;

    public interface JsSubTemplateModel extends TemplateModel {
        @AllowClientUpdates
        String getFoo();

        void setFoo(String value);
    }

    public JsSubTemplate() {
        getModel().setFoo("bar");
    }

}
