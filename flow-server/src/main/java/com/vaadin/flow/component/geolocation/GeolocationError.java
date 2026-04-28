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
 * A failed location reading: the request did not produce a
 * {@link GeolocationPosition}.
 * <p>
 * This is one of the three possible values of a
 * {@link GeolocationTracker#valueSignal()} signal, and one of the two values a
 * {@link Geolocation#get} callback can receive. Typical application code
 * switches on {@link #errorCode()} to react to the specific reason:
 *
 * <pre>
 * ui.getGeolocation().get(result -&gt; {
 *     if (result instanceof GeolocationError err) {
 *         switch (err.errorCode()) {
 *         case PERMISSION_DENIED -&gt;
 *             showExplanation("Location is blocked for this site.");
 *         case POSITION_UNAVAILABLE -&gt;
 *             showRetry("Could not determine your location.");
 *         case TIMEOUT -&gt; showRetry("Location request took too long.");
 *         case UNKNOWN -&gt; showGenericError(err.message());
 *         }
 *     }
 * });
 * </pre>
 *
 * The raw numeric {@link #code()} is also exposed for logging and for safe
 * handling of future browser codes that the enum does not yet know about.
 *
 * @param code
 *            the raw numeric error code as reported by the browser.
 *            Applications should usually call {@link #errorCode()} instead of
 *            comparing this directly
 * @param message
 *            a human-readable message from the browser. Mainly useful for
 *            logging — the wording is not standardised and should not be shown
 *            to end users as-is
 */
public record GeolocationError(int code,
        String message) implements GeolocationOutcome {

    /**
     * Returns the error reason as a typed enum suitable for exhaustive
     * {@code switch}. Returns {@link GeolocationErrorCode#UNKNOWN} if the
     * browser reports a numeric code this version of Flow does not recognise.
     *
     * @return the matching {@link GeolocationErrorCode}, or
     *         {@link GeolocationErrorCode#UNKNOWN} when the raw {@link #code()}
     *         is not one of the known values
     */
    public GeolocationErrorCode errorCode() {
        return GeolocationErrorCode.fromCode(code);
    }
}
