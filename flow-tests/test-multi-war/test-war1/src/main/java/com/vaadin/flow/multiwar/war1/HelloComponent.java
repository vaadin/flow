package com.vaadin.flow.multiwar.war1;

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
