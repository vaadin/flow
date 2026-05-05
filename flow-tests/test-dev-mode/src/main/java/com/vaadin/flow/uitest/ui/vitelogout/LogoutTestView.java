/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.vitelogout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

/**
 * View for testing Vite logout redirect behavior.
 * <p>
 * Contains a logout button that sets the location to the session-ended route
 * and invalidates the session. The test verifies that Vite's page reload
 * doesn't cancel the server-initiated redirect when the session is invalidated.
 */
@Route("com.vaadin.flow.uitest.ui.vitelogout.LogoutTestView")
public class LogoutTestView extends Div {

    public LogoutTestView() {
        Span marker = new Span("Logout Test View");
        marker.setId("logout-test-marker");

        NativeButton logoutButton = new NativeButton("Logout", e -> {
            UI.getCurrent().getPage().setLocation(
                    "/view/com.vaadin.flow.uitest.ui.vitelogout.SessionEndedView");
            VaadinSession.getCurrent().getSession().invalidate();
        });
        logoutButton.setId("logout-button");

        add(marker, logoutButton);
    }
}
