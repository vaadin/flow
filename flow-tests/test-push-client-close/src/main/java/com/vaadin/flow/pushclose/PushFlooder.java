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
package com.vaadin.flow.pushclose;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.PushConnection;

/**
 * Pushes large updates to every UI one after another for as long as its client
 * stays connected, so that whenever the client closes its push connection, a
 * push message is still being written to it.
 * <p>
 * The updates start as soon as the UI is created, so that the test does not
 * have to navigate it to a view first.
 */
@Component
public class PushFlooder implements VaadinServiceInitListener {

    /**
     * Size of the text sent with each update. Large enough that a client that
     * stops reading makes the server's writes back up within a few updates.
     */
    static final int UPDATE_SIZE = 64 * 1024;

    /**
     * The character the updated text is made of.
     */
    static final char FILLER = 'x';

    /**
     * Upper bound for how long one UI is pushed to, so that a client that goes
     * away without the server noticing does not leave it pushing forever.
     */
    private static final long MAX_FLOOD_MS = TimeUnit.SECONDS.toMillis(30);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInit -> {
            UI ui = uiInit.getUI();
            Div content = new Div();
            ui.add(content);
            Thread.ofVirtual().name("push-flood")
                    .start(() -> new Flood(ui, content).run());
        });
    }

    private static final class Flood implements Runnable {

        private final UI ui;
        private final Div content;
        private boolean connectedOnce;
        private volatile boolean disconnected;

        private Flood(UI ui, Div content) {
            this.ui = ui;
            this.content = content;
        }

        @Override
        public void run() {
            String filler = String.valueOf(FILLER).repeat(UPDATE_SIZE);
            long deadline = System.currentTimeMillis() + MAX_FLOOD_MS;
            for (int i = 0; !disconnected
                    && System.currentTimeMillis() < deadline; i++) {
                String text = i + filler;
                try {
                    ui.access(() -> {
                        trackConnection();
                        content.setText(text);
                    }).get(MAX_FLOOD_MS, TimeUnit.MILLISECONDS);
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    // The UI is gone, so there is nothing to push to
                    return;
                }
            }
        }

        private void trackConnection() {
            PushConnection connection = ui.getInternals().getPushConnection();
            if (connection != null && connection.isConnected()) {
                connectedOnce = true;
            } else if (connectedOnce) {
                disconnected = true;
            }
        }
    }
}
