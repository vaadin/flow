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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.communication.PushConnection;
import com.vaadin.flow.shared.communication.PushMode;

@Route("com.vaadin.flow.uitest.ui.ActivatePushView")
public class ActivatePushView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        // Shows a marker over push once the client has connected, which it
        // only can if it loaded the push script from the right url
        Thread pusher = new Thread(() -> {
            try {
                while (!isConnected(ui)) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            ui.access(() -> {
                Div pushed = new Div("Pushed");
                pushed.setId("pushed");
                add(pushed);
            });
        });
        pusher.setDaemon(true);
        pusher.start();
    }

    private static boolean isConnected(UI ui) {
        PushConnection connection = ui.getInternals().getPushConnection();
        return connection != null && connection.isConnected();
    }
}
