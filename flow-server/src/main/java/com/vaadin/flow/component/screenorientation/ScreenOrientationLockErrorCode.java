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

/**
 * Typed reasons why a screen-orientation lock request can fail.
 * <p>
 * Returned by {@link ScreenOrientationLockError#errorCode()}. Prefer this enum
 * over comparing the raw {@code DOMException} name string — an exhaustive
 * {@code switch} catches missing branches at compile time.
 * <p>
 * Each constant holds the {@code DOMException} name the browser uses for that
 * failure. Applications rarely need {@link #domExceptionName()} directly; it is
 * exposed for logging and for round-tripping with
 * {@link ScreenOrientationLockError#name()}.
 */
public enum ScreenOrientationLockErrorCode {
    /**
     * The browser does not implement the Screen Orientation API, or does not
     * allow locking on this device (for example a desktop that has no physical
     * orientation to lock to). Corresponds to a {@code NotSupportedError}
     * {@code DOMException}.
     */
    NOT_SUPPORTED("NotSupportedError"),

    /**
     * The document is not in fullscreen, which most browsers require before
     * honoring a lock request. Corresponds to a {@code SecurityError}
     * {@code DOMException}.
     */
    SECURITY("SecurityError"),

    /**
     * A newer lock or unlock call superseded this request before the browser
     * resolved it. Corresponds to an {@code AbortError} {@code DOMException}.
     */
    ABORT("AbortError"),

    /**
     * The browser rejected the request for a reason that does not match a more
     * specific code, or the executeJs round-trip itself failed.
     * {@link ScreenOrientationLockError#name()} still holds the raw
     * {@code DOMException} name and
     * {@link ScreenOrientationLockError#message()} the underlying message for
     * diagnostics.
     */
    UNKNOWN("");

    private final String domExceptionName;

    ScreenOrientationLockErrorCode(String domExceptionName) {
        this.domExceptionName = domExceptionName;
    }

    /**
     * Returns the {@code DOMException} name associated with this code, or the
     * empty string for {@link #UNKNOWN}. Mainly useful for logging or
     * interoperating with {@link ScreenOrientationLockError#name()}.
     *
     * @return the {@code DOMException} name, or {@code ""} for {@link #UNKNOWN}
     */
    public String domExceptionName() {
        return domExceptionName;
    }

    /**
     * Looks up the enum constant for a raw {@code DOMException} name, returning
     * {@link #UNKNOWN} if the name is not one this version of Flow recognises.
     * This is how {@link ScreenOrientationLockError#errorCode()} behaves — it
     * maps known names to constants and surfaces unknown ones as
     * {@link #UNKNOWN} rather than throwing.
     *
     * @param name
     *            the {@code DOMException} name from the browser
     * @return the matching constant, or {@link #UNKNOWN} if unrecognised
     */
    public static ScreenOrientationLockErrorCode fromName(String name) {
        for (ScreenOrientationLockErrorCode code : values()) {
            if (code != UNKNOWN && code.domExceptionName.equals(name)) {
                return code;
            }
        }
        return UNKNOWN;
    }
}
