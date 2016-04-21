/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.event;

import java.util.function.Consumer;

import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.HasElement;
import com.vaadin.ui.UI;

/**
 * A class that contains events, listeners and handlers specific to the
 * {@link UI} class.
 *
 * @since 7.2
 * @author Vaadin Ltd
 */
public interface UIEvents {

    /**
     * An event that is fired whenever a client polls the server for
     * asynchronous UI updates.
     *
     * @since 7.2
     * @author Vaadin Ltd
     */
    @DomEvent(PollEvent.DOM_EVENT_NAME)
    public static class PollEvent extends ComponentEvent<UI> {
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

    /**
     * The interface for adding and removing {@link PollEvent} listeners.
     * <p>
     * By implementing this interface, a class publicly announces that it is
     * able to send {@link PollEvent PollEvents} whenever the client sends a
     * periodic poll message to the client, to check for asynchronous
     * server-side modifications.
     *
     * @since 7.2
     * @see UI#setPollInterval(int)
     */
    public interface PollNotifier extends HasElement {
        /**
         * Add a poll listener.
         * <p>
         * The listener is called whenever the client polls the server for
         * asynchronous UI updates.
         *
         * @see UI#setPollInterval(int)
         * @param listener
         *            the listener to add
         */
        void addPollListener(Consumer<PollEvent> listener);
    }

}
