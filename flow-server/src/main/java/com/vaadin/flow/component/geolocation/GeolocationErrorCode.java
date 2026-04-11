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
package com.vaadin.flow.component.geolocation;

/**
 * Typed reasons why a geolocation request can fail.
 * <p>
 * Returned by {@link GeolocationError#errorCode()}. Prefer this enum over
 * comparing raw numbers — exhaustive {@code switch} catches missing branches at
 * compile time, and a {@code case null} arm lets the application handle future
 * browser codes gracefully without losing the compile-time check.
 * <p>
 * Each constant holds an integer identifier. Applications rarely need to look
 * at {@link #code()} directly; it is exposed for logging and for round-tripping
 * with {@link GeolocationError#code()}.
 */
public enum GeolocationErrorCode {
    /**
     * The user refused to share their location — typically by clicking "Block"
     * on the browser's permission dialog, or by previously denying permission
     * for this origin. Once denied, the browser will keep returning this code
     * until the user re-enables location for the site in their browser
     * settings.
     */
    PERMISSION_DENIED(1),

    /**
     * The browser tried to determine a position but failed — for example
     * because the device has no GPS fix and no network positioning is
     * available, or because the operating system refused the request. Usually
     * transient; showing a retry button is a reasonable response.
     */
    POSITION_UNAVAILABLE(2),

    /**
     * The browser did not produce a position within the time budget set by
     * {@link GeolocationOptions}. Either the user is in a difficult-to-locate
     * environment or the timeout was too tight.
     */
    TIMEOUT(3);

    private final int code;

    GeolocationErrorCode(int code) {
        this.code = code;
    }

    /**
     * Returns the integer identifier of this error code. Mainly useful for
     * logging or interoperating with {@link GeolocationError#code()}.
     *
     * @return the numeric code
     */
    public int code() {
        return code;
    }

    /**
     * Looks up the enum constant for a raw numeric code, returning {@code null}
     * if the code is not one this version of Flow recognises. This is how
     * {@link GeolocationError#errorCode()} behaves — it maps known codes to
     * constants and surfaces unknown future codes as {@code null} rather than
     * throwing.
     *
     * @param code
     *            the numeric error code
     * @return the matching constant, or {@code null} if unknown
     */
    public static GeolocationErrorCode fromCode(int code) {
        for (GeolocationErrorCode c : values()) {
            if (c.code == code) {
                return c;
            }
        }
        return null;
    }
}
