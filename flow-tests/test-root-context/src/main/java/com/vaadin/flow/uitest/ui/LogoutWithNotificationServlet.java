/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.SystemMessagesProvider;
import com.vaadin.flow.server.VaadinServlet;

@WebServlet("/logout-with-notification/*")
public class LogoutWithNotificationServlet extends VaadinServlet {

    @Route("logout-with-notification-view")
    public static class LogoutRedirectView extends Div {
        public LogoutRedirectView() {
            NativeButton logoutButton = new NativeButton("logout", ev -> {
                UI.getCurrent().getPage().setLocation("redirect-target-view");
                UI.getCurrent().getSession().close();
            });
            add(logoutButton);
        }
    }

    @Route("redirect-target-view")
    public static class RedirectTargetView extends Div {
        public RedirectTargetView() {
            Span span = new Span("Redirect Target Span");
            span.setId("redirect-target-span");
            add(span);
        }
    }

    @Override
    protected void servletInitialized() throws ServletException {
        super.servletInitialized();
        getService().setSystemMessagesProvider(
                (SystemMessagesProvider) systemMessagesInfo -> {
                    final CustomizedSystemMessages systemMessages = new CustomizedSystemMessages();
                    systemMessages.setSessionExpiredNotificationEnabled(true);
                    return systemMessages;
                });
    }
}
