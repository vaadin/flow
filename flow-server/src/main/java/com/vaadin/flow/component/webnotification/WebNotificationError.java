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
package com.vaadin.flow.component.webnotification;

import java.io.Serializable;
import java.util.Objects;

/**
 * Why the browser did not show a notification. Delivered to the listeners added
 * with {@link WebNotificationHandle#addErrorListener}.
 *
 * @param reason
 *            what kind of failure it is, for deciding what to do about it
 * @param message
 *            a description of the failure, for logging
 */
public record WebNotificationError(Reason reason,
        String message) implements Serializable {

    /**
     * Creates an error.
     *
     * @param reason
     *            what kind of failure it is, not {@code null}
     * @param message
     *            a description of the failure, not {@code null}
     */
    public WebNotificationError {
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    /**
     * What kind of failure prevented a notification from being shown.
     */
    public enum Reason {

        /**
         * The browser has no Notifications API in this context. Same condition
         * as {@link WebNotificationPermission#UNSUPPORTED}.
         */
        UNSUPPORTED,

        /**
         * The user has not granted permission to show notifications. Check
         * {@link WebNotification#permissionSignal()} before showing a
         * notification, and ask for permission with
         * {@link WebNotification#onClick}.
         */
        NOT_ALLOWED,

        /**
         * The browser failed to show the notification for another reason. For
         * example, Chrome on Android only shows notifications through a service
         * worker.
         */
        UNKNOWN
    }
}
