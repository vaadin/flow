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
package com.vaadin.flow.component.trigger.internal;

import java.io.Serializable;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.webnotification.WebNotificationError;
import com.vaadin.flow.component.webnotification.WebNotificationError.Reason;

/**
 * Reads a {@link WebNotificationError} from what the client-side notification
 * bridge reports through a return channel. Shared by
 * {@link ShowWebNotificationAction} and the server-side notification handle,
 * which receive the same wire format.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class WebNotificationErrorReader implements Serializable {

    private WebNotificationErrorReader() {
        // utility class
    }

    /**
     * Reads the error the client reports through a notification's return
     * channel as {@code ('error', reason, message)}. A reason this server does
     * not know, for example from a newer client, is read as
     * {@link Reason#UNKNOWN}.
     *
     * @param args
     *            the arguments the client passed to the channel, not
     *            {@code null}
     * @return the error, not {@code null}
     */
    public static WebNotificationError read(ArrayNode args) {
        JsonNode reason = args.get(1);
        JsonNode message = args.get(2);
        Reason parsed;
        try {
            parsed = reason == null ? Reason.UNKNOWN
                    : Reason.valueOf(reason.asString());
        } catch (IllegalArgumentException e) {
            parsed = Reason.UNKNOWN;
        }
        return new WebNotificationError(parsed,
                message == null || message.isNull() ? "" : message.asString());
    }
}
