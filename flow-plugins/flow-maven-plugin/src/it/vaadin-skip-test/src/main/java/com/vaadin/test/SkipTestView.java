package com.vaadin.test;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("")
public class SkipTestView extends Div {
    public SkipTestView() {
        setText("Skip test view");
    }
}