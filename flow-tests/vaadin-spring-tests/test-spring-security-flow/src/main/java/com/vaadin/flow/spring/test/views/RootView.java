package com.vaadin.flow.spring.test.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route(value = "")
public class RootView extends Div {

    public RootView() {
        setId("root");
        add(new Span("root view"));
    }
}
