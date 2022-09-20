package com.vaadin.flow.spring.boot.interop;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Hello World")
@Route(value = "")
public class HelloWorldView extends Div {

    private NativeButton sayHello;
    private Div output;

    public HelloWorldView() {
        output = new Div();
        output.setId("output");
        sayHello = new NativeButton("Say hello");
        sayHello.addClickListener(e -> {
            output.setText("Hello");
        });

        add(sayHello, output);
    }

}
