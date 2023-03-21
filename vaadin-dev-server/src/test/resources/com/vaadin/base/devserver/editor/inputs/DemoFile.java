package com.vaadin.base.devserver.editor.inputs;

import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import jakarta.servlet.ServletContext;

@Route(value = "demo", layout = MainLayout.class)
public class DemoFile extends HorizontalLayout {

    private TextField name;
    private Button sayHello;

    public DemoFile(ServletContext servletContext) {
        name = new TextField("Your name");

        sayHello = new Button("Say hello1");

        Button sayHello2 = new Button("Say hello2");

        Button sayHello3;
        sayHello3 = new Button("Say hello3");

        Button sayHello4 = new Button();
        sayHello4.setText("Say hello4");

        Button sayHello5 = new Button();

        sayHello.addClickListener(e -> {
            Notification.show("Hello " + name.getValue());
        });
        sayHello.addClickShortcut(Key.ENTER);

        setMargin(true);
        setVerticalComponentAlignment(Alignment.END, name, sayHello);

        add(name, sayHello, sayHello2, sayHello3, sayHello4);
        add(sayHello5, new Button("Say hello6"));
    }

}