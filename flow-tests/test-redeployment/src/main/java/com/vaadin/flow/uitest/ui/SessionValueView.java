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

import java.util.UUID;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

@Route(value = "com.vaadin.flow.uitest.ui.SessionValueView")
public class SessionValueView extends AbstractReloadView {

    // Test that session values outside VaadinSession are preserved on reload
    public SessionValueView() {

        addTriggerButton();

        WrappedSession session = VaadinSession.getCurrent().getSession();
        String customAttribute = (String) session
                .getAttribute("custom-attribute");
        if (customAttribute == null) {
            customAttribute = UUID.randomUUID().toString();
            session.setAttribute("custom-attribute", customAttribute);
        }
        Div div = new Div();
        div.setId("customAttribute");
        div.setText("The custom value in the session is: " + customAttribute);
        add(div);

        addViewId();
    }
}
