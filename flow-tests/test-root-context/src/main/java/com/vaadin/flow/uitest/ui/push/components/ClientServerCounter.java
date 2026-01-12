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
package com.vaadin.flow.uitest.ui.push.components;

import java.util.Timer;
import java.util.TimerTask;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

public class ClientServerCounter extends Div {

    public static final String CLIENT_COUNTER_ID = "clientCounter";

    public static final String STOP_TIMER_ID = "stopTimer";

    public static final String START_TIMER_ID = "startTimer";

    public static final String SERVER_COUNTER_ID = "serverCounter";

    public static final String INCREMENT_BUTTON_ID = "incrementCounter";

    private final Timer timer = new Timer(true);

    private TimerTask task;
    private int clientCounter = 0;
    private int serverCounter = 0;

    private Element serverCounterElement;

    public ClientServerCounter() {
        spacer();

        // Client counter
        getElement().appendChild(ElementFactory
                .createDiv("Client counter (click 'increment' to update):"));
        Element lbl = ElementFactory.createDiv(clientCounter + "")
                .setAttribute("id", CLIENT_COUNTER_ID);
        getElement().appendChild(lbl);

        NativeButton button = new NativeButton("Increment", e -> {
            clientCounter++;
            lbl.setText(clientCounter + "");
        });
        button.setId(INCREMENT_BUTTON_ID);

        getElement().appendChild(button.getElement());
        spacer();

        /*
         * Server counter
         */
        getElement().appendChild(ElementFactory.createDiv(
                "Server counter (updates each second by server thread):"));
        serverCounterElement = ElementFactory.createDiv().setAttribute("id",
                SERVER_COUNTER_ID);
        serverCounterElement.setText(serverCounter + "");
        getElement().appendChild(serverCounterElement);

        NativeButton startTimer = new NativeButton("Start timer");
        startTimer.addClickListener(e -> {
            serverCounter = 0;
            if (task != null) {
                task.cancel();
            }
            task = new TimerTask() {
                @Override
                public void run() {
                    startTimer.getUI().get().access(() -> {
                        serverCounter++;
                        serverCounterElement.setText(serverCounter + "");
                    });
                }
            };
            timer.scheduleAtFixedRate(task, 1000, 1000);
        });
        startTimer.setId(START_TIMER_ID);
        getElement().appendChild(startTimer.getElement());

        Element stopTimer = ElementFactory.createButton("Stop timer")
                .setAttribute("id", STOP_TIMER_ID);
        stopTimer.setText("Stop timer");
        stopTimer.addEventListener("click", e -> {
            if (task != null) {
                task.cancel();
                task = null;
            }
        });
        getElement().appendChild(stopTimer);

    }

    protected void spacer() {
        getElement().appendChild(ElementFactory.createHr());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        timer.cancel();
    }

}
