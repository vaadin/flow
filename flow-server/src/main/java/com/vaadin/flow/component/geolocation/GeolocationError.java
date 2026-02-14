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
 * Represents an error from the browser's Geolocation API.
 *
 * @param code
 *            the error code, one of {@link #PERMISSION_DENIED},
 *            {@link #POSITION_UNAVAILABLE}, or {@link #TIMEOUT}
 * @param message
 *            a human-readable error message
 */
public record GeolocationError(int code, String message) {

    /**
     * The user denied the request for geolocation.
     */
    public static final int PERMISSION_DENIED = 1;

    /**
     * The position could not be determined.
     */
    public static final int POSITION_UNAVAILABLE = 2;

    /**
     * The request to get the position timed out.
     */
    public static final int TIMEOUT = 3;
}
