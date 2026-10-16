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

/**
 * Whether the browser lets the application show notifications.
 * <p>
 * Held by {@link WebNotification#permissionSignal()}. Reading the value never
 * shows a browser prompt. Typical usage:
 * <ul>
 * <li>{@link #PROMPT} — show an "Enable notifications" button bound with
 * {@link WebNotification#onClick} that asks for permission.</li>
 * <li>{@link #GRANTED} — call {@link WebNotification#show(String)} when there
 * is something to tell the user.</li>
 * <li>{@link #DENIED} — explain how to re-enable notifications in the browser's
 * site settings, and fall back to in-page messages.</li>
 * <li>{@link #UNSUPPORTED} — hide notification settings entirely and fall back
 * to in-page messages.</li>
 * </ul>
 */
public enum WebNotificationPermission {

    /**
     * No value has been reported by the browser yet. Used only as the initial
     * value of the signal before the bootstrap handshake delivers the real one,
     * so it is essentially never observed in application code.
     */
    UNKNOWN,

    /**
     * The user has not decided yet. Asking for permission with
     * {@link WebNotificationBinding#requestPermission()} shows the browser's
     * permission prompt.
     */
    PROMPT,

    /**
     * The user allowed notifications for the application.
     */
    GRANTED,

    /**
     * The user blocked notifications for the application. Browsers do not show
     * the permission prompt again; the user has to allow notifications in the
     * browser's site settings, after which the value changes to
     * {@link #GRANTED} when the user returns to the tab.
     */
    DENIED,

    /**
     * The browser has no Notifications API in this context: the page is not
     * served from a secure context (HTTPS or {@code localhost}), or the browser
     * does not implement the API.
     */
    UNSUPPORTED
}
