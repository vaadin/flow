/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component;

/**
 * An event that is fired whenever a client polls the server for asynchronous UI
 * updates.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@DomEvent(PollEvent.DOM_EVENT_NAME)
public class PollEvent extends ComponentEvent<UI> {
    public static final String DOM_EVENT_NAME = "ui-poll";

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param ui
     *            the source UI
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     *            side, <code>false</code> otherwise
     */
    public PollEvent(UI ui, boolean fromClient) {
        super(ui, fromClient);
    }

}
