package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;

@Route("component-test")
public class ComponentTestView extends Div {

    public ComponentTestView() {
        Button button = new Button("Click me",
                event -> Notification.show("Clicked!"));

        add(button);
    }
}
