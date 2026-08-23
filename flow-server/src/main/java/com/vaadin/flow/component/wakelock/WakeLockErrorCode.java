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
package com.vaadin.flow.component.wakelock;

/**
 * Reason a
 * {@link WakeLock#request(com.vaadin.flow.function.SerializableConsumer)
 * WakeLock.request(onError)} call failed.
 *
 * @since 25.2
 */
public enum WakeLockErrorCode {
    /**
     * The Screen Wake Lock API is unusable in this context — the browser does
     * not implement it, or the page is served over an insecure connection (not
     * HTTPS or {@code localhost}). This matches the
     * {@link WakeLockAvailability#UNSUPPORTED} availability hint, so you can
     * hide wake-lock controls up front rather than letting the request fail.
     */
    UNSUPPORTED,

    /**
     * The browser refused the request, usually because the tab is currently
     * hidden, a permissions policy blocks wake lock in this frame, or the
     * operating system's power-management settings prevent it.
     */
    NOT_ALLOWED,

    /**
     * The request failed for a reason that does not match a more specific code.
     * See {@link WakeLockError#message()} for the underlying browser message.
     */
    UNKNOWN
}
