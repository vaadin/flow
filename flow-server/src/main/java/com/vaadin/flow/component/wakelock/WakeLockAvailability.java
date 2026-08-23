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

import com.vaadin.flow.component.UI;

/**
 * Whether the browser can hold a screen wake lock in the current page context.
 * <p>
 * Held by {@link WakeLock#availabilitySignal()}. Reading the value does not
 * call the browser API — it reports whether the next {@link WakeLock#request()}
 * has any chance of succeeding.
 * <p>
 * Typical usage: hide a "keep screen on" toggle entirely when the value is
 * {@link #UNSUPPORTED} (no user action can change this), and otherwise offer it
 * and observe {@link WakeLock#activeSignal(UI)} for the actual state.
 *
 * @since 25.2
 */
public enum WakeLockAvailability {
    /**
     * The browser exposes the Screen Wake Lock API and the page is served from
     * a secure context, so {@link WakeLock#request()} has a chance of
     * succeeding. Whether the browser actually grants the lock still depends on
     * page visibility, battery state, and other transient conditions — observe
     * {@link WakeLock#activeSignal(UI)} for the actual outcome.
     */
    SUPPORTED,

    /**
     * The Screen Wake Lock API is not usable in the current page context — the
     * browser does not implement it, or the page is served over an insecure
     * connection (plain HTTP rather than HTTPS or {@code localhost}). No user
     * action can change this; applications should hide wake-lock-related
     * controls entirely instead of offering a button that would always fail.
     */
    UNSUPPORTED,

    /**
     * The browser has not yet reported a value. This is the seed value held
     * during the brief window between UI attach and the bootstrap response
     * being processed; in practice applications see it only as a transient
     * state.
     */
    UNKNOWN
}
