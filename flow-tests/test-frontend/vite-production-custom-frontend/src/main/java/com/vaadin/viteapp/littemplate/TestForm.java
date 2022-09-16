package com.vaadin.viteapp.littemplate;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;

@JsModule("./lit-templates/test-form.js")
@Tag("test-form")
public class TestForm extends LitTemplate {

    @Id
    private Div div;

    public TestForm() {
        div.setText("foo");
    }
}
