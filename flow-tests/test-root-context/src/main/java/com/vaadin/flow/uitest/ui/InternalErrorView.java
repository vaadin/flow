/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
                "close-session",
                event -> VaadinSession.getCurrent().close());

        NativeButton enableNotificationButton = createButton(
                "Enable session expired notification", "enable-notification",
                event -> enableSessionExpiredNotification());

        NativeButton causeExceptionButton = createButton("Cause exception",
                "cause-exception",
                event -> showInternalError());

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
