package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Tag("simple-lit-template-no-shadow-root")
@JsModule("lit/simple-lit-template-no-shadow-root.js")
@NpmPackage(value = "lit-element", version = "2.1.0")
@Route(value = "com.vaadin.flow.uitest.ui.littemplate.SimpleLitTemplateNoShadowRootView", layout = ViewTestLayout.class)
public class SimpleLitTemplateNoShadowRootView extends LitTemplate {

    @Id
    public NativeButton mappedButton;
    @Id
    public Div label;

    public SimpleLitTemplateNoShadowRootView() {
        getElement().setProperty("text", "Client button");
        mappedButton.setText("Server button");
        mappedButton.addClickListener(e -> {
            label.setText("Hello from server component event listener");
        });

    }

    @ClientCallable
    public void sayHello() {
        label.setText("Hello from ClientCallable");
    }

}
