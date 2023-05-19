package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;

@Tag("injected-lit-template")
@JsModule("./lit-templates/InjectedTemplate.js")
public class InjectedTemplateView extends LitTemplate {

    public InjectedTemplateView() {
    }
}
