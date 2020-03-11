package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.littemplate.SimpleLitTemplateShadowRootView", layout = ViewTestLayout.class)
public class SimpleLitTemplateShadowRootView extends Div {

    public SimpleLitTemplateShadowRootView() {
        add(new SimpleLitTemplateShadowRoot());
    }
}
