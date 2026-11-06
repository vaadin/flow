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
package com.vaadin.flow.quarkus.it.sseaddon;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * Pushes large messages for a while to a UI opened with the {@code flood}
 * location, to fill the buffers on the way to a client that does not read.
 * Starts when the UI is created, as a client that only opens the event stream
 * never navigates.
 */
public class FloodStarter implements VaadinServiceInitListener {

    private static final String LARGE = "x".repeat(512 * 1024);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        // Short, so that the client notices a silently lost stream within
        // seconds in PushIT. Read by the add-on when a stream opens.
        System.setProperty("vaadin.pushSseKeepAliveInterval", "2");
        event.getSource().addUIInitListener(uiInit -> {
            VaadinRequest request = VaadinRequest.getCurrent();
            if (request != null
                    && "flood".equals(request.getParameter("location"))) {
                flood(uiInit.getUI());
            }
        });
    }

    private static void flood(UI ui) {
        Div target = new Div();
        ui.add(target);
        Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < 200; i++) {
                    int round = i;
                    ui.access(() -> target.setText(round + LARGE));
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
