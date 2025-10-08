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

import java.util.concurrent.locks.Lock;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

@Route("push")
public class PushView extends Div {

    private final class SayWorld implements Runnable {
        private final UI ui;

        private SayWorld(UI ui) {
            this.ui = ui;
        }

        @Override
        public void run() {
            // We can acquire the lock after the request started this thread is
            // processed
            // Needed to make sure that this is sent as a push message
            Lock lockInstance = ui.getSession().getLockInstance();
            lockInstance.lock();
            lockInstance.unlock();

            ui.access(() -> {
                Paragraph world = new Paragraph("World");
                world.setId("world");
                add(world);
            });
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);

        // Fallback transport is forced to websocket so that we either get a
        // websocket connection or no push connection at all
        ui.getPushConfiguration().setFallbackTransport(Transport.WEBSOCKET);
        add(new NativeButton("Say hello", e -> {
            add(new Paragraph("Hello"));
            new Thread(new SayWorld(ui)).start();
        }));
    }
}
