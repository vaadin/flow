package com.vaadin.flow.npmtest.ui.littemplate;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route(value = "SimpleLitTemplateView")
@JsModule("lit/simple-lit-template.js")
public class SimpleLitTemplateView extends Div {

    public SimpleLitTemplateView() {
        add(new SimpleLitTemplate());
    }
}
