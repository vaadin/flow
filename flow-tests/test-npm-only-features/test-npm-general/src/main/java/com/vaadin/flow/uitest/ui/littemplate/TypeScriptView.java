package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * A Designer generated component for the hello-world-view template.
 *
 * Designer will add and remove fields with @Id mappings but does not overwrite
 * or otherwise change this file.
 */
@Route(value = "com.vaadin.flow.uitest.ui.littemplate.TypeScriptView", layout = ViewTestLayout.class)
@JsModule("./lit/type-script-view.ts")
@Tag("type-script-view")
public class TypeScriptView extends LitTemplate {

    @Id
    public NativeButton mappedButton;
    @Id
    public Div label;

    public TypeScriptView() {
        mappedButton.setText("Server button");
        mappedButton.addClickListener(e -> {
            label.setText("Hello from server component event listener");
        });

    }
}
