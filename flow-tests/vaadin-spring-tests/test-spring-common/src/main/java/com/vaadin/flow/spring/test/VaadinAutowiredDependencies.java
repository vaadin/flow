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
package com.vaadin.flow.spring.test;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("vaadin-autowired-deps")
public class VaadinAutowiredDependencies extends Div {

    @Autowired
    private UI ui;

    @Autowired
    private VaadinSession session;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Div uiId = new Div();
        uiId.setId("ui-injected");
        uiId.setText(String.valueOf(ui.getUIId()) + ui.hashCode());

        Div currentUi = new Div();
        currentUi.setText(String.valueOf(UI.getCurrent().getUIId())
                + UI.getCurrent().hashCode());
        currentUi.setId("ui-current");

        Div sessionDiv = new Div();
        sessionDiv.setText(ui.getCsrfToken() + session.hashCode());
        sessionDiv.setId("session-injected");

        Div sessionCurrent = new Div();
        sessionCurrent.setText(UI.getCurrent().getCsrfToken()
                + VaadinSession.getCurrent().hashCode());
        sessionCurrent.setId("session-current");

        add(uiId, currentUi, sessionDiv, sessionCurrent);

    }
}
