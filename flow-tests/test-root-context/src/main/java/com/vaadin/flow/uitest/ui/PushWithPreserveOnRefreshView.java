package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@PreserveOnRefresh
@Push
@Route("com.vaadin.flow.uitest.ui.PushWithPreserveOnRefreshView")
public class PushWithPreserveOnRefreshView extends Div {

    private int times = 0;

    public PushWithPreserveOnRefreshView() {
        NativeButton button = new NativeButton("click me", event -> log(
                "Button has been clicked " + (++times) + " times"));
        add(button);
    }

    private void log(String msg) {
        Div div = new Div();
        div.addClassName("log");
        div.setText(msg);
        add(div);
    }

}