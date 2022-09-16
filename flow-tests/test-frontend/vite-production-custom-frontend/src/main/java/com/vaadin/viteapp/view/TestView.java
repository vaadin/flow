package com.vaadin.viteapp.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.viteapp.littemplate.TestForm;

@Route("")
public class TestView extends Div {

    public TestView() {
        add(new TestForm());
    }

}
