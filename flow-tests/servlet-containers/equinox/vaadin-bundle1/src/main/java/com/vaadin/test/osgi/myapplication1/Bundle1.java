package com.vaadin.test.osgi.myapplication1;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("")
public class Bundle1 extends Div {

    public Bundle1() {
        Input textField = new Input();
        textField.getElement().setAttribute("placeholder",
                "Type your name here:");
        textField.setId("bundle-input");

        NativeButton button = new NativeButton("Click Me");
        button.addClickListener(event -> add(
                new Label("Thanks " + textField.getValue() + ", it works!")));
        button.setId("bundle-button");

        add(textField, button);
    }
}
