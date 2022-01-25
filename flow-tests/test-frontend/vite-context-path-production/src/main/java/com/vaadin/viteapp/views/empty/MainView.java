package com.vaadin.viteapp.views.empty;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends Div {
    public MainView() {
        add(new H2("Hello world!"));
    }
}
