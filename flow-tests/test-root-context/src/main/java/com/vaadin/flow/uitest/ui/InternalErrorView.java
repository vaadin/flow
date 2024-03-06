/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.io.IOException;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.DefaultSystemMessagesProvider;
import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.JsonConstants;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
@Route("com.vaadin.flow.uitest.ui.InternalErrorView")
public class InternalErrorView extends AbstractDivView {

    public InternalErrorView() {
        Div message = new Div();
        message.setId("message");

        NativeButton updateMessageButton = createButton("Update", "update",
                event -> message.setText("Updated"));

        NativeButton closeSessionButton = createButton("Close session",
                "close-session", event -> VaadinSession.getCurrent().close());

        NativeButton enableNotificationButton = createButton(
                "Enable session expired notification", "enable-notification",
                event -> enableSessionExpiredNotification());

        NativeButton causeExceptionButton = createButton("Cause exception",
                "cause-exception", event -> showInternalError());

        NativeButton resetSystemMessagesButton = createButton(
                "Reset system messages", "reset-system-messages",
                event -> resetSystemMessages());

        add(message, updateMessageButton, closeSessionButton,
                enableNotificationButton, causeExceptionButton,
                resetSystemMessagesButton);
    }

    private void showInternalError() {
        SystemMessages systemMessages = VaadinService.getCurrent()
                .getSystemMessages(getLocale(), VaadinRequest.getCurrent());

        showCriticalNotification(systemMessages.getInternalErrorCaption(),
                systemMessages.getInternalErrorMessage(), "",
                systemMessages.getInternalErrorURL());

    }

    protected void showCriticalNotification(String caption, String message,
            String details, String url) {
        VaadinService service = VaadinService.getCurrent();
        VaadinResponse response = VaadinService.getCurrentResponse();

        try {
            service.writeUncachedStringResponse(response,
                    JsonConstants.JSON_CONTENT_TYPE,
                    VaadinService.createCriticalNotificationJSON(caption,
                            message, details, url));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enableSessionExpiredNotification() {
        CustomizedSystemMessages sysMessages = new CustomizedSystemMessages();
        sysMessages.setSessionExpiredNotificationEnabled(true);

        VaadinService.getCurrent()
                .setSystemMessagesProvider(systemMessagesInfo -> sysMessages);
    }

    private void resetSystemMessages() {
        VaadinService.getCurrent()
                .setSystemMessagesProvider(DefaultSystemMessagesProvider.get());
    }
}
