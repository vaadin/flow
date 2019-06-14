package com.vaadin.flow.multiwar.war2;

import com.vaadin.flow.component.html.Div;

public class HelloComponent extends Div {
    public HelloComponent() {
        setId("hello");
        setText("Hello from " + getClass().getName());
        addClickListener(e -> {
            setText("Hello " + getText());
        });
    }
}
