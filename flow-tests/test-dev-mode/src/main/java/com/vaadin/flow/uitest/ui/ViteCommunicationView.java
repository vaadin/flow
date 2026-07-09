/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
                e -> ui.getPage().executeJs(
                        "window.importmetahot.send('test-event', {foo: 'bar'});"));
        sendMessage.setId("send");
        Div response = new Div();
        response.setId("response");

        add(sendMessage, response);

    }

}
