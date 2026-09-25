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
 * when the client asks the server to resynchronize a UI, on the request thread
 * while the session is locked and before the UI is resynchronized.
 * <p>
 * The client asks for a resynchronization when it detects that it missed a
 * message from the server, typically because of a bad network connection or a
 * push connection that was closed by a proxy. The server then sends the full
 * state of the UI to the client.
 */
public class UIResynchronizationEvent extends EventObject {

    /**
     * Creates a new event.
     *
     * @param ui
     *            the UI to resynchronize, not {@code null}
     */
    public UIResynchronizationEvent(UI ui) {
        super(ui);
    }

    /**
     * Gets the UI to resynchronize.
     *
     * @return the UI, not {@code null}
     */
    public UI getUI() {
        return (UI) getSource();
    }
}
