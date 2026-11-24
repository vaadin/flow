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
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Lets a test close the push connection from the server, so that the client
 * opens a new one.
 */
@CustomPush(transport = Transport.SERVER_SENT_EVENTS)
@Route(value = "com.vaadin.flow.uitest.ui.push.SsePushReconnectView", layout = ViewTestLayout.class)
public class SsePushReconnectView extends BasicPushSseView {

    public static final String CLOSE_PUSH_LATER_ID = "close-push-later";

    /**
     * Delay before the push connection is closed, giving the test time to
     * prepare the reconnect.
     */
    public static final int CLOSE_DELAY_MS = 2000;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        NativeButton closePushLater = new NativeButton(
                "Close push connection in " + CLOSE_DELAY_MS + " ms",
                event -> new Thread(() -> {
                    try {
                        Thread.sleep(CLOSE_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    ui.access(() -> ui.getInternals().getPushConnection()
                            .disconnect());
                }).start());
        closePushLater.setId(CLOSE_PUSH_LATER_ID);
        add(closePushLater);
    }
}
