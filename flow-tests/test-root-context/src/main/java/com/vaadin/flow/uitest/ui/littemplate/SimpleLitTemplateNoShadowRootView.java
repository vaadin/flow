package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.littemplate.SimpleLitTemplateNoShadowRootView", layout = ViewTestLayout.class)
public class SimpleLitTemplateNoShadowRootView extends Div {

    public SimpleLitTemplateNoShadowRootView() {
        add(new SimpleLitTemplateNoShadowRoot());
    }
}
