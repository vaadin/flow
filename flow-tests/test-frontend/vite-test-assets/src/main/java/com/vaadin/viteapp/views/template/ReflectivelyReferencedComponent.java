package com.vaadin.viteapp.views.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;

@Tag(ReflectivelyReferencedComponent.TAG)
@JsModule("./templates/ReflectivelyReferencedComponent.ts")
public class ReflectivelyReferencedComponent extends LitTemplate {

    public static final String TAG = "reflectively-referenced-component";
}
