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

package com.vaadin.flow.server.communication;

import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.SystemMessages;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Serializes miscellaneous metadata to JSON.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MetadataWriter implements Serializable {

    private int timeoutInterval = -1;

    /**
     * Creates a JSON object containing metadata related to the given UI.
     *
     * @param ui
     *            The UI whose metadata to write.
     * @param repaintAll
     *            Whether the client should repaint everything.
     * @param async
     *            True if this message is sent by the server asynchronously,
     *            false if it is a response to a client message.
     * @param messages
     *            a {@link SystemMessages} containing client-side error
     *            messages.
     * @return JSON object with the metadata
     *
     */
    public JsonObject createMetadata(UI ui, boolean repaintAll, boolean async,
            SystemMessages messages) {
        JsonObject meta = Json.createObject();

        if (repaintAll) {
            meta.put("repaintAll", true);
        }

        if (async) {
            meta.put("async", true);
        }

        // meta instruction for client to enable auto-forward to
        // sessionExpiredURL after timer expires.
        if (messages != null && messages.getSessionExpiredMessage() == null
                && messages.getSessionExpiredCaption() == null
                && messages.isSessionExpiredNotificationEnabled()
                && ui.getSession().getSession() != null) {
            int newTimeoutInterval = ui.getSession().getSession()
                    .getMaxInactiveInterval();
            if (repaintAll || (timeoutInterval != newTimeoutInterval)) {
                String url = messages.getSessionExpiredURL();
                if (url == null) {
                    url = "";
                }
                int redirectInterval = newTimeoutInterval + 15;

                JsonObject redirect = Json.createObject();
                redirect.put("interval", redirectInterval);
                redirect.put("url", url);

                meta.put("timedRedirect", redirect);
            }
            timeoutInterval = newTimeoutInterval;
        }

        return meta;
    }
}
