package com.vaadin.viteapp.views.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag(PolymerComponent.TAG)
@JsModule("./templates/PolymerComponent.ts")
public class PolymerComponent extends PolymerTemplate<TemplateModel> {
    public static final String TAG = "polymer-component";

    @Id("label")
    private Span label;

    public void setLabel(String value) {
        label.setText(value);
    }
}
