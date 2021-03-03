package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route(value = "secured")
public class SecuredView extends Div {

    public SecuredView() {
        setId("secured");
        add(new Span("secured"));
    }
}
