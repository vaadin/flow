package com.vaadin.test.osgi.myapplication;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("")
public class Bundle2 extends Div {

    public void Bundle2() {
        Input textField = new Input();
        textField.getElement().setAttribute("placeholder",
                "Type your name here:");
        textField.setId("bundle-2-input");

        Label message = new Label();
        message.setId("message");

        NativeButton button = new NativeButton("Click Me");
        button.addClickListener(event -> message
                .setText("Thanks " + textField.getValue() + ", it works!"));

        add(textField, button, message);
    }

}
