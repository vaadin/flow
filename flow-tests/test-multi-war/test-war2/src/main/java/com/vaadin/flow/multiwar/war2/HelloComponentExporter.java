package com.vaadin.flow.multiwar.war2;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class HelloComponentExporter extends WebComponentExporter<HelloComponent> {

    public HelloComponentExporter() {
        super("hello-war2");
    }

    @Override
    public void configureInstance(WebComponent<HelloComponent> webComponent, HelloComponent component) {
    }

}
