package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

public class PushToggleComponentVisibilityUI extends UI {

    @Override
    protected void init(VaadinRequest request) {
        getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        getPushConfiguration().setTransport(Transport.WEBSOCKET_XHR);

        Div mainLayout = new Div();

        Div label = new Div();
        label.setText("Please wait");
        label.setId("label");
        label.setVisible(false);
        mainLayout.add(label);

        NativeButton button = new NativeButton("Hide me for 3 seconds");
        button.setId("hide");

        button.addClickListener(event1 -> {
            button.setVisible(false);
            label.setVisible(true);

            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                access(() -> {
                    button.setVisible(true);
                    label.setVisible(false);
                    push();
                });
            }).start();
        });
        mainLayout.add(button);

        add(mainLayout);
    }

}