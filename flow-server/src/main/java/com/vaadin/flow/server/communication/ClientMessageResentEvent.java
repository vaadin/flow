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
package com.vaadin.flow.server.communication;

import java.util.EventObject;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * Event fired through the {@link VaadinService#getEventBus() service event bus}
 * when the client sent the last message the server processed once more, on the
 * request thread while the session is locked.
 * <p>
 * The client re-sends a message when it did not get a response to it, for
 * example because of a timeout. The message is not processed again: the server
 * responds with the response it sent the first time.
 */
public class ClientMessageResentEvent extends EventObject {

    /**
     * Creates a new event.
     *
     * @param ui
     *            the UI the message was sent to, not {@code null}
     */
    public ClientMessageResentEvent(UI ui) {
        super(ui);
    }

    /**
     * Gets the UI the message was sent to.
     *
     * @return the UI, not {@code null}
     */
    public UI getUI() {
        return (UI) getSource();
    }
}
