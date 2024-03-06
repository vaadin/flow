/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.BasicComponentView", layout = ViewTestLayout.class)
public class BasicComponentView extends AbstractDivView {

    public static final String TEXT = "This is the basic component view text component with some tags: <b><html></body>";
    public static final String BUTTON_TEXT = "Click me";
    public static final String DIV_TEXT = "Hello world";

    @Override
    protected void onShow() {
        getElement().getStyle().set("margin", "1em");
        getElement().setAttribute("id", "root");

        Text text = new Text(TEXT);

        Input input = new Input();
        input.setPlaceholder("Synchronized on change event");

        NativeButton button = new NativeButton(BUTTON_TEXT, e -> {
            Div greeting = new Div();
            greeting.addClassName("thankYou");
            String buttonText = e.getSource().getElement().getText();

            greeting.setText("Thank you for clicking \"" + buttonText
                    + "\" at (" + e.getClientX() + "," + e.getClientY()
                    + ")! The field value is " + input.getValue());

            greeting.addClickListener(e2 -> remove(greeting));
            add(greeting);
        });

        Div helloWorld = new Div();
        helloWorld.setText(DIV_TEXT);
        helloWorld.addClassName("hello");
        helloWorld.setId("hello-world");
        helloWorld.addClickListener(e -> {
            helloWorld.setText("Stop touching me!");
            helloWorld.getElement().getClassList().clear();
        });
        Style s = helloWorld.getElement().getStyle();
        s.set("color", "red");
        s.set("fontWeight", "bold");

        add(text, helloWorld, button, input);
    }

}
