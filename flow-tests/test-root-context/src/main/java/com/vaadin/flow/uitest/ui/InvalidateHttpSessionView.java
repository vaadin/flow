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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

@Route("com.vaadin.flow.uitest.ui.InvalidateHttpSessionView")
public class InvalidateHttpSessionView extends Div {

    private static class SessionId {

        private String id;

        private SessionId(String id) {
            this.id = id;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        VaadinService service = attachEvent.getSession().getService();
        String id = attachEvent.getSession().getSession().getId();

        Div div = new Div();
        div.setText(id);
        div.setId("current-session-id");

        add(div);

        SessionId closedSessionId = attachEvent.getSession().getService()
                .getContext().getAttribute(SessionId.class);
        if (closedSessionId != null) {
            div = new Div();
            div.setText(closedSessionId.id);
            div.setId("invalidated-session-id");
            add(div);
        }

        service.addSessionDestroyListener(event -> {
            SessionId sessionId = new SessionId(id);
            service.getContext().setAttribute(SessionId.class, sessionId);
        });
        NativeButton button = new NativeButton("Invalidate HTTP session",
                event -> attachEvent.getSession().getSession().invalidate());
        add(button);
        button.setId("invalidate-session");
    }
}
