package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.PopupView", layout = ViewTestLayout.class)
public class PopupView extends AbstractDivView {

    public PopupView() {

        NativeButton button = new NativeButton("Open popup");
        button.addClickListener(event -> getElement().executeJavaScript("window.open('https://www.vaadin.com')"));

        add(button);
    }

}
