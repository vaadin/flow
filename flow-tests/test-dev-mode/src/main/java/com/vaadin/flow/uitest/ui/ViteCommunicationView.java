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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ViteCommunicationView", layout = ViewTestLayout.class)
@JsModule("./vite-communication.ts")
public class ViteCommunicationView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        Page page = ui.getPage();
        page.executeJs(
                """
                        window.importmetahot.on('test-event-response', (event) => {
                            document.querySelector('#response').innerText += 'Got event test-event-response with data ' + JSON.stringify(event) + '';
                        });
                        """);

        NativeButton sendMessage = new NativeButton("Send message",
                e -> ui.getPage()
                        .executeJs("window.importmetahot.send('test-event', {foo: 'bar'});"));
        sendMessage.setId("send");
        Div response = new Div();
        response.setId("response");

        add(sendMessage, response);

    }

}
