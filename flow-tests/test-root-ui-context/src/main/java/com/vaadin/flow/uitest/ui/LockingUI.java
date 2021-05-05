package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.uitest.servlet.CustomDeploymentConfiguration;
import com.vaadin.flow.uitest.servlet.CustomDeploymentConfiguration.Conf;

@CustomDeploymentConfiguration({
        @Conf(name = "heartbeatInterval", value = "2") })
public class LockingUI extends UI {

    public static final String LOCKING_ENDED = "Locking has ended";
    public static final String ALL_OK = "All is fine";
    private Div message;

    @Override
    protected void init(VaadinRequest request) {
        message = new Div();
        message.setId("message");
        message.setText("default");

        NativeButton lockButton = new NativeButton("Lock UI for too long");
        lockButton.addClickListener(e -> {
            setHeartBeats();
            message.setText(LOCKING_ENDED);
        });
        NativeButton checkButton = new NativeButton("Test communication",
                e -> message.setText(ALL_OK));

        lockButton.setId("lock");
        checkButton.setId("check");

        add(lockButton, checkButton, message);
    }

    private void setHeartBeats() {
        int heartbeatInterval = VaadinService.getCurrent()
                .getDeploymentConfiguration().getHeartbeatInterval();
        try {
            // Wait for 4 heartbeats
            long timeout = heartbeatInterval * 1000;
            for (int i = 0; i < 4; ++i) {
                Thread.sleep(timeout);
            }

        } catch (InterruptedException e1) {
            throw new RuntimeException("Timeout should not get interrupted.");
        }
    }
}
