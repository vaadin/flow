package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;

@Route("resync-listener")
public class ResyncListenerView extends Div {

    public ResyncListenerView() {
        Button resynch = new Button("Resynchronize",
                event -> UI.getCurrent().getInternals().incrementServerId());
        resynch.setId("resync");

        Button button = new Button("Click me");
        button.addClickListener(event -> {
            Notification.show("Works");
        });
        button.setId("button");

        add(resynch, button);
    }
}
