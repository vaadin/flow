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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.RequestListenerView", layout = ViewTestLayout.class)
public class RequestListenerView extends Div {

    private static final String ADD_REQUEST_LISTENER = """
            const log = this;
            const report = (text) => {
              const line = document.createElement('div');
              line.className = 'log';
              line.textContent = text;
              log.appendChild(line);
            };
            const clients = window.Vaadin.Flow.clients;
            const client = clients[Object.keys(clients).find((key) => key !== 'TypeScript')];
            client.addRequestListener({
              requestStarted: (event) => report('started ' + event.requestId),
              responseReceived: (event) => report('response ' + event.requestId),
              requestEnded: (event) => report('ended ' + event.requestId)
            });
            """;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Div log = new Div();
        log.setId("log");
        log.getElement().executeJs(ADD_REQUEST_LISTENER);

        NativeButton button = new NativeButton("Send request",
                event -> add(new Div("Request handled")));
        button.setId("send");
        add(button, log);
    }
}
