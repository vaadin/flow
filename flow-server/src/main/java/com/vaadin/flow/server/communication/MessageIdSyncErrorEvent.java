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
 * when the client sent a message with an id the server did not expect, on the
 * request thread while the session is locked.
 * <p>
 * The message is not processed and the client is shown the session
 * synchronization error. This typically happens when the server that handled
 * the UI stopped before the UI state was stored, and the client continues on
 * another server with an older state.
 *
 * @see ServerRpcHandler.MessageIdSyncException
 */
public class MessageIdSyncErrorEvent extends EventObject {

    private final int expectedId;
    private final int receivedId;

    /**
     * Creates a new event.
     *
     * @param ui
     *            the UI the message was sent to, not {@code null}
     * @param expectedId
     *            the message id the server expected
     * @param receivedId
     *            the message id the client sent
     */
    public MessageIdSyncErrorEvent(UI ui, int expectedId, int receivedId) {
        super(ui);
        this.expectedId = expectedId;
        this.receivedId = receivedId;
    }

    /**
     * Gets the UI the message was sent to.
     *
     * @return the UI, not {@code null}
     */
    public UI getUI() {
        return (UI) getSource();
    }

    /**
     * Gets the message id the server expected.
     *
     * @return the expected message id
     */
    public int getExpectedId() {
        return expectedId;
    }

    /**
     * Gets the message id the client sent.
     *
     * @return the received message id
     */
    public int getReceivedId() {
        return receivedId;
    }
}
