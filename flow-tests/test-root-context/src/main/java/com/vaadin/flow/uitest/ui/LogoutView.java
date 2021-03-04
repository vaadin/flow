/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.SystemMessagesProvider;
import com.vaadin.flow.server.VaadinService;

@Route("com.vaadin.flow.uitest.ui.LogoutView")
public class LogoutView extends Div {

    public LogoutView() {
        NativeButton logoutButton = new NativeButton("logout", ev -> {
            UI.getCurrent().getPage().setLocation(BaseHrefView.class.getName());
            UI.getCurrent().getSession().close();
        });
        logoutButton.setId("logout-button");
        add(logoutButton);

        NativeButton notificationLogoutButton = new NativeButton(
                "logout-with-notification", ev -> {
                    VaadinService.getCurrent().setSystemMessagesProvider(
                            (SystemMessagesProvider) systemMessagesInfo -> {
                                final CustomizedSystemMessages systemMessages = 
                                                new CustomizedSystemMessages();
                                systemMessages
                                        .setSessionExpiredNotificationEnabled(
                                                true);
                                return systemMessages;
                            });

                    UI.getCurrent().getPage()
                            .setLocation(BaseHrefView.class.getName());
                    UI.getCurrent().getSession().close();
                });
        notificationLogoutButton.setId("logout-with-notification-button");
        add(notificationLogoutButton);
    }

}
