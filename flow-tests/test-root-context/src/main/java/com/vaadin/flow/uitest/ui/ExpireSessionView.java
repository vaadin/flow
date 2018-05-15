package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

@Route("com.vaadin.flow.uitest.ui.ExpireSessionView")
public class ExpireSessionView extends AbstractDivView {

    public ExpireSessionView() {
        Div message = new Div();
        message.setId("message");

        NativeButton updateMessageButton = new NativeButton("Update",
                event -> message.setText("Updated"));
        updateMessageButton.setId("update");

        NativeButton closeSessionButton = new NativeButton("Close session",
                event -> VaadinSession.getCurrent().close());
        closeSessionButton.setId("close-session");

        NativeButton enableNotificationButton = new NativeButton(
                "Enable session expired notification",
                event -> enableSessionExpiredNotification());
        enableNotificationButton.setId("enable-notification");

        add(message, updateMessageButton, closeSessionButton,
                enableNotificationButton);
    }

    private void enableSessionExpiredNotification() {
        CustomizedSystemMessages sysMessages = new CustomizedSystemMessages();
        sysMessages.setSessionExpiredNotificationEnabled(true);

        VaadinService.getCurrent()
                .setSystemMessagesProvider(systemMessagesInfo -> sysMessages);
    }
}
