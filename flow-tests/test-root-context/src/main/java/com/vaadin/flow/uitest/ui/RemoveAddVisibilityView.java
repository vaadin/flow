package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.RemoveAddVisibilityView")
public class RemoveAddVisibilityView extends Div {

    public RemoveAddVisibilityView() {
        Span hidden = new Span("Initially hidden");
        hidden.setVisible(false);

        NativeButton toggle = new NativeButton("Toggle Visibility", event -> {
            remove(hidden);
            add(hidden);
            hidden.setVisible(!hidden.isVisible());
        });
        toggle.setId("toggle");

        add(toggle, hidden);
    }
}