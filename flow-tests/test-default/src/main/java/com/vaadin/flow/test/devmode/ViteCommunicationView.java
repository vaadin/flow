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
package com.vaadin.flow.test.devmode;

import java.io.Serializable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.router.Route;

@Route("vite-communication")
@JsModule("./vite-communication.ts")
public class ViteCommunicationView extends Div {

    @JsDefinition
    public interface ViteJs extends Serializable {

        @JsExpression("""
                window.importmetahot.on('test-event-response', (event) => {
                    document.querySelector('#response').innerText += 'Got event test-event-response with data ' + JSON.stringify(event) + '';
                });
                """)
        void listenForResponse();

        @JsExpression("window.importmetahot.send('test-event', {foo: 'bar'});")
        void sendTestEvent();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.getPage().executeJs(ViteJs.class).listenForResponse();

        NativeButton sendMessage = new NativeButton("Send message",
                e -> ui.getPage().executeJs(ViteJs.class).sendTestEvent());
        sendMessage.setId("send");
        Div response = new Div();
        response.setId("response");

        add(sendMessage, response);
    }

}
