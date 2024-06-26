/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("js-sub-template")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/JsSubTemplate.html")
@JsModule("JsSubTemplate.js")
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
