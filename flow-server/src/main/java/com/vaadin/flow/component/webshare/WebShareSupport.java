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
package com.vaadin.flow.component.webshare;

/**
 * Whether the browser exposes the Web Share API ({@code navigator.share}).
 * <p>
 * Held by {@link WebShare#supportSignal()}. Reading the value does not show any
 * browser dialog — it reports whether a subsequent {@link WebShareBinding#share
 * share} action would be able to invoke the native share sheet, or whether the
 * call would silently no-op because the API is missing in the current browser
 * context.
 * <p>
 * Typical usage:
 * <ul>
 * <li>{@link #SUPPORTED} — show a "Share" button that triggers a share
 * action.</li>
 * <li>{@link #UNSUPPORTED} — fall back to a copy-link or social-network
 * affordance; the native sheet is unavailable in this browser.</li>
 * <li>{@link #UNKNOWN} — only seen in the brief window before the first
 * bootstrap handshake completes; treat the same as {@link #UNSUPPORTED} until a
 * real value arrives.</li>
 * </ul>
 */
public enum WebShareSupport {

    /**
     * No value has been reported by the browser yet. Used only as the initial
     * value of the signal before the first client handshake delivers the real
     * one. In normal request handling the signal is seeded before any user code
     * (UI initialization, {@code UIInitListener}, component attach) runs, so
     * this value is essentially never observed in practice; once a real value
     * has arrived, the signal never returns to {@code UNKNOWN}.
     */
    UNKNOWN,

    /**
     * The browser exposes {@code navigator.share}; share actions bound to a
     * user gesture will invoke the native share sheet.
     */
    SUPPORTED,

    /**
     * The browser does not expose {@code navigator.share}; share actions
     * silently reject. Most desktop Firefox builds and older browsers fall in
     * this bucket.
     */
    UNSUPPORTED
}
