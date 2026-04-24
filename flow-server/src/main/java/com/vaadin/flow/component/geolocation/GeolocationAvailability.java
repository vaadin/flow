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
 * Whether the browser can read the user's location right now, and if so what
 * permission state the origin has.
 * <p>
 * Returned by {@link Geolocation#availabilitySignal()}. Reading the value does
 * <b>not</b> show a permission dialog — it reports whether a dialog would
 * appear on the next {@link Geolocation#get} or {@link Geolocation#track} call,
 * or whether the call would fail regardless because the feature is unusable in
 * this context.
 * <p>
 * Typical usage:
 * <ul>
 * <li>{@link #UNSUPPORTED} — hide the location control entirely; no user action
 * can change this.</li>
 * <li>{@link #DENIED} — pre-explain to the user why location is blocked and how
 * to re-enable it in the browser settings.</li>
 * <li>{@link #GRANTED} — safe to auto-fetch silently on return visits.</li>
 * <li>{@link #PROMPT} / {@link #UNKNOWN} — wait for an explicit user action
 * (click a button) before triggering a browser prompt.</li>
 * </ul>
 */
public enum GeolocationAvailability {
    /**
     * The user has previously granted permission for this origin. A subsequent
     * {@link Geolocation#get} or {@link Geolocation#track} call will proceed
     * without showing a dialog.
     */
    GRANTED,

    /**
     * The user has previously denied permission for this origin. Subsequent
     * calls will fail with {@link GeolocationErrorCode#PERMISSION_DENIED}
     * without re-prompting. The only way to change this is for the user to
     * update the site permissions in their browser settings.
     */
    DENIED,

    /**
     * Permission has not yet been decided. The next {@link Geolocation#get} or
     * {@link Geolocation#track} call will show the browser's permission dialog.
     */
    PROMPT,

    /**
     * The browser did not report a permission state. Treat this as "do not
     * auto-fetch; wait for the user to take an explicit action". Safari always
     * returns this value because it does not implement permission querying for
     * geolocation.
     */
    UNKNOWN,

    /**
     * The geolocation feature is unusable in the current page context — the
     * page is served over an insecure connection (plain HTTP rather than HTTPS
     * or {@code localhost}), or it is embedded in an iframe whose
     * Permissions-Policy blocks geolocation. No user action can change this, so
     * applications should hide location-related controls entirely instead of
     * offering a button that would always fail.
     */
    UNSUPPORTED
}
