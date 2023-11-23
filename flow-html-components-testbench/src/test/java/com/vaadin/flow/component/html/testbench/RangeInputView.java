package com.vaadin.flow.component.html.testbench;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.RangeInput;
import com.vaadin.flow.router.Route;

@Route("RangeInput")
public class RangeInputView extends Div {

    public RangeInputView() {
        Div log = new Div();
        log.setId("log");

        RangeInput input = new RangeInput();
        input.setId("input");
        input.addValueChangeListener(e -> {
            log.setText("Value is '" + input.getValue() + "'");
        });
        add(log, input);
    }
}
