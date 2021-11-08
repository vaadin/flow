package com.vaadin.viteapp.views.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;

@Tag(LitComponent.TAG)
@JsModule("./templates/LitComponent.ts")
public class LitComponent extends LitTemplate {

    public static final String TAG = "lit-component";

    @Id("label")
    private Span label;

    public void setLabel(String value) {
        label.setText(value);
    }
}
