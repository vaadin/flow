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
 * The browser's record of whether this origin is allowed to read the user's
 * location.
 * <p>
 * Returned by
 * {@link Geolocation#queryPermission(com.vaadin.flow.function.SerializableConsumer)}.
 * Querying this value does <b>not</b> show a permission dialog — it reports
 * whether a dialog would appear on the next {@link Geolocation#get} /
 * {@link Geolocation#track} call. Use it to silently auto-fetch on return
 * visits when the user has already granted permission, while first-time
 * visitors see the view undisturbed until they click something.
 */
public enum GeolocationPermission {
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
    UNKNOWN
}
