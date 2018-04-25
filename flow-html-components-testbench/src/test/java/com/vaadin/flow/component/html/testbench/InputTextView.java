package com.vaadin.flow.component.html.testbench;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.router.Route;

@Route("InputText")
public class InputTextView extends Div {

    public InputTextView() {
        Div log = new Div();
        log.setId("log");

        Input input = new Input();
        input.setId("input");
        input.addValueChangeListener(e -> {
            log.setText("Value is '" + input.getValue() + "'");
        });
        add(log, input);
    }
}
