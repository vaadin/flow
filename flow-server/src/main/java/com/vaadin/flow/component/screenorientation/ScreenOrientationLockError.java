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
package com.vaadin.flow.component.screenorientation;

import java.io.Serializable;

/**
 * Describes a failed screen-orientation lock request.
 * <p>
 * Fields mirror the {@code DOMException} the browser rejects
 * {@code screen.orientation.lock()} with. Prefer {@link #errorCode()} over
 * comparing the raw {@link #name()} string; it maps the common
 * {@code DOMException} names to a typed {@link ScreenOrientationLockErrorCode}:
 * <ul>
 * <li>{@code "NotSupportedError"} →
 * {@link ScreenOrientationLockErrorCode#NOT_SUPPORTED} — the browser does not
 * implement the Screen Orientation API at all, or does not allow locking on
 * this device.</li>
 * <li>{@code "SecurityError"} → {@link ScreenOrientationLockErrorCode#SECURITY}
 * — the document is not in fullscreen, which most browsers require for
 * locking.</li>
 * <li>{@code "AbortError"} → {@link ScreenOrientationLockErrorCode#ABORT} — a
 * newer lock or unlock call superseded this one.</li>
 * </ul>
 *
 * @param name
 *            the {@code DOMException} name, e.g. {@code "SecurityError"}
 * @param message
 *            the {@code DOMException} message, suitable for logging
 */
public record ScreenOrientationLockError(String name,
        String message) implements Serializable {

    /**
     * Returns the typed reason for this failure, mapping the raw
     * {@link #name()} {@code DOMException} name to a
     * {@link ScreenOrientationLockErrorCode}. Unrecognised names map to
     * {@link ScreenOrientationLockErrorCode#UNKNOWN} rather than throwing; the
     * raw {@link #name()} remains available for logging.
     *
     * @return the typed error code, never {@code null}
     */
    public ScreenOrientationLockErrorCode errorCode() {
        return ScreenOrientationLockErrorCode.fromName(name);
    }
}
