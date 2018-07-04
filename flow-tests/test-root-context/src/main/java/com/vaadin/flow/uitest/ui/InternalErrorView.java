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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.DefaultSystemMessagesProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * @author Vaadin Ltd.
 */
@Route("com.vaadin.flow.uitest.ui.InternalErrorView")
public class InternalErrorView extends AbstractDivView {

    public InternalErrorView() {
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

        NativeButton causeExceptionButton = new NativeButton("Cause exception",
                event -> System.out.println(1 / 0));
        causeExceptionButton.setId("cause-exception");

        NativeButton resetSystemMessagesButton = new NativeButton(
                "Reset system messages", event -> resetSystemMessages());
        resetSystemMessagesButton.setId("reset-system-messages");

        add(message, updateMessageButton, closeSessionButton,
                enableNotificationButton, causeExceptionButton,
                resetSystemMessagesButton);
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
