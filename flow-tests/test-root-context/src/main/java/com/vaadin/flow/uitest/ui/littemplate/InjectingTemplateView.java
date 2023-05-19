package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.littemplate.InjectingTemplateView")
@Tag("injecting-lit-template")
@JsModule("./lit-templates/InjectingTemplate.js")
public class InjectingTemplateView extends LitTemplate {

    @Id("injected")
    private InjectedTemplateView view;

    public InjectingTemplateView() {
        setId("injecting");
    }
}
