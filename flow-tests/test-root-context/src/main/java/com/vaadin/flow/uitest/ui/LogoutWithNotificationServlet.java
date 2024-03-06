/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

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
