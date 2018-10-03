package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.VaadinRequest;

public class NoRouterUI extends UI {

    @Override
    public Router getRouter() {
        return null;
    }

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);
        add(new NativeButton("Hello", e -> {
            Div div = new Div();
            div.setClassName("response");
            div.setText("Hello");
            add(div);
        }));
    }
}
